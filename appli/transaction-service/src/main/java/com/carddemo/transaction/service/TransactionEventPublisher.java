package com.carddemo.transaction.service;

import com.carddemo.shared.event.TransactionCreatedEvent;
import com.carddemo.transaction.entity.Transaction;
import com.carddemo.transaction.entity.TransactionOutbox;
import com.carddemo.transaction.repository.TransactionOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Transaction Event Publisher
 *
 * Publishes transaction events using the Outbox Pattern.
 * The event is written to the outbox table in the same transaction
 * as the business operation, ensuring atomicity.
 *
 * Replaces: CICS SYNCPOINT + MQ PUT atomicity (Unit of Work)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final TransactionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publish a TransactionCreatedEvent (saved to outbox)
     *
     * This method should be called within the same @Transactional boundary
     * as the transaction creation, ensuring atomicity.
     */
    @Transactional
    public void publishTransactionCreated(Transaction transaction) {
        TransactionCreatedEvent event = TransactionCreatedEvent.create(
                transaction.getTransactionId(),
                transaction.getAccountId(),
                transaction.getTransactionType(),
                transaction.getTransactionCategory(),
                transaction.getTransactionSource(),
                transaction.getTransactionDesc(),
                transaction.getTransactionAmount(),
                transaction.getMerchantId(),
                transaction.getMerchantName(),
                transaction.getMerchantCity(),
                transaction.getMerchantZip(),
                transaction.getCardNumber(),
                transaction.getTransactionDate(),
                transaction.getTransactionTime()
        );

        saveToOutbox(event);
    }

    /**
     * Save event to outbox table
     */
    private void saveToOutbox(TransactionCreatedEvent event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);

            TransactionOutbox outbox = TransactionOutbox.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .aggregateId(String.valueOf(event.getAccountId()))
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);

            log.info("Event saved to outbox: eventId={}, type={}, aggregateId={}",
                    event.getEventId(), event.getEventType(), event.getAccountId());

        } catch (Exception e) {
            log.error("Failed to save event to outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish transaction event", e);
        }
    }
}
