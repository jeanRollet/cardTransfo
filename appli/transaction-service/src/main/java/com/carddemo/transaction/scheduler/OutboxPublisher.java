package com.carddemo.transaction.scheduler;

import com.carddemo.shared.event.DomainEvent;
import com.carddemo.shared.event.KafkaTopics;
import com.carddemo.transaction.entity.TransactionOutbox;
import com.carddemo.transaction.repository.TransactionOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Outbox Publisher
 *
 * Periodically polls the outbox table and publishes events to Kafka.
 * Implements the Outbox Pattern for reliable event publishing.
 *
 * Replaces: CICS triggered transaction patterns for MQ message dispatch
 */
@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final TransactionOutboxRepository outboxRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Poll outbox every 1 second and publish pending events
     */
    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<TransactionOutbox> pendingEvents = outboxRepository.findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} pending events from outbox", pendingEvents.size());

        for (TransactionOutbox outbox : pendingEvents) {
            publishEvent(outbox);
        }
    }

    /**
     * Publish a single event to Kafka
     */
    @Transactional
    public void publishEvent(TransactionOutbox outbox) {
        try {
            // Convert payload back to DomainEvent
            DomainEvent event = objectMapper.convertValue(
                    outbox.getPayload(),
                    DomainEvent.class
            );

            String topic = KafkaTopics.TRANSACTIONS;
            String key = outbox.getAggregateId();

            CompletableFuture<SendResult<String, DomainEvent>> future =
                    kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    handleSuccess(outbox, result);
                } else {
                    handleFailure(outbox, ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing event from outbox: id={}, error={}",
                    outbox.getId(), e.getMessage(), e);
            outbox.markFailed();
            outboxRepository.save(outbox);
        }
    }

    private void handleSuccess(TransactionOutbox outbox, SendResult<String, DomainEvent> result) {
        log.info("Event published to Kafka: eventId={}, topic={}, partition={}, offset={}",
                outbox.getEventId(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

        outbox.markPublished();
        outboxRepository.save(outbox);
    }

    private void handleFailure(TransactionOutbox outbox, Throwable ex) {
        log.error("Failed to publish event to Kafka: eventId={}, error={}",
                outbox.getEventId(), ex.getMessage());
        // Keep as PENDING for retry on next poll
    }

    /**
     * Log statistics every 5 minutes
     */
    @Scheduled(fixedDelay = 300000)
    public void logStats() {
        long pending = outboxRepository.countByStatus(TransactionOutbox.STATUS_PENDING);
        long published = outboxRepository.countByStatus(TransactionOutbox.STATUS_PUBLISHED);
        long failed = outboxRepository.countByStatus(TransactionOutbox.STATUS_FAILED);

        if (pending > 0 || failed > 0) {
            log.info("Outbox stats: pending={}, published={}, failed={}", pending, published, failed);
        }
    }
}
