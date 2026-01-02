package com.carddemo.card.scheduler;

import com.carddemo.card.entity.CardOutbox;
import com.carddemo.card.repository.CardOutboxRepository;
import com.carddemo.shared.event.DomainEvent;
import com.carddemo.shared.event.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Card Outbox Publisher
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final CardOutboxRepository outboxRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<CardOutbox> pendingEvents = outboxRepository.findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} pending card events from outbox", pendingEvents.size());

        for (CardOutbox outbox : pendingEvents) {
            publishEvent(outbox);
        }
    }

    @Transactional
    public void publishEvent(CardOutbox outbox) {
        try {
            DomainEvent event = objectMapper.convertValue(outbox.getPayload(), DomainEvent.class);

            String topic = KafkaTopics.CARDS;
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
            log.error("Error publishing card event from outbox: id={}, error={}",
                    outbox.getId(), e.getMessage(), e);
            outbox.markFailed();
            outboxRepository.save(outbox);
        }
    }

    private void handleSuccess(CardOutbox outbox, SendResult<String, DomainEvent> result) {
        log.info("Card event published to Kafka: eventId={}, topic={}, partition={}, offset={}",
                outbox.getEventId(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

        outbox.markPublished();
        outboxRepository.save(outbox);
    }

    private void handleFailure(CardOutbox outbox, Throwable ex) {
        log.error("Failed to publish card event to Kafka: eventId={}, error={}",
                outbox.getEventId(), ex.getMessage());
    }

    @Scheduled(fixedDelay = 300000)
    public void logStats() {
        long pending = outboxRepository.countByStatus(CardOutbox.STATUS_PENDING);
        long published = outboxRepository.countByStatus(CardOutbox.STATUS_PUBLISHED);
        long failed = outboxRepository.countByStatus(CardOutbox.STATUS_FAILED);

        if (pending > 0 || failed > 0) {
            log.info("Card outbox stats: pending={}, published={}, failed={}", pending, published, failed);
        }
    }
}
