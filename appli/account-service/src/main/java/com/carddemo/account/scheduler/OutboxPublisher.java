package com.carddemo.account.scheduler;

import com.carddemo.account.entity.AccountOutbox;
import com.carddemo.account.repository.AccountOutboxRepository;
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
 * Account Outbox Publisher
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final AccountOutboxRepository outboxRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<AccountOutbox> pendingEvents = outboxRepository.findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} pending account events from outbox", pendingEvents.size());

        for (AccountOutbox outbox : pendingEvents) {
            publishEvent(outbox);
        }
    }

    @Transactional
    public void publishEvent(AccountOutbox outbox) {
        try {
            DomainEvent event = objectMapper.convertValue(outbox.getPayload(), DomainEvent.class);

            String topic = KafkaTopics.ACCOUNTS;
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
            log.error("Error publishing account event from outbox: id={}, error={}",
                    outbox.getId(), e.getMessage(), e);
            outbox.markFailed();
            outboxRepository.save(outbox);
        }
    }

    private void handleSuccess(AccountOutbox outbox, SendResult<String, DomainEvent> result) {
        log.info("Account event published to Kafka: eventId={}, topic={}, partition={}, offset={}",
                outbox.getEventId(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

        outbox.markPublished();
        outboxRepository.save(outbox);
    }

    private void handleFailure(AccountOutbox outbox, Throwable ex) {
        log.error("Failed to publish account event to Kafka: eventId={}, error={}",
                outbox.getEventId(), ex.getMessage());
    }

    @Scheduled(fixedDelay = 300000)
    public void logStats() {
        long pending = outboxRepository.countByStatus(AccountOutbox.STATUS_PENDING);
        long published = outboxRepository.countByStatus(AccountOutbox.STATUS_PUBLISHED);
        long failed = outboxRepository.countByStatus(AccountOutbox.STATUS_FAILED);

        if (pending > 0 || failed > 0) {
            log.info("Account outbox stats: pending={}, published={}, failed={}", pending, published, failed);
        }
    }
}
