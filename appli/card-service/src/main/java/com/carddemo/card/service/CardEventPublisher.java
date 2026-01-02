package com.carddemo.card.service;

import com.carddemo.card.entity.CardOutbox;
import com.carddemo.card.entity.CreditCard;
import com.carddemo.card.repository.CardOutboxRepository;
import com.carddemo.shared.event.CardStatusChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Card Event Publisher
 *
 * Publishes card events using the Outbox Pattern.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CardEventPublisher {

    private final CardOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publish a CardStatusChangedEvent (saved to outbox)
     */
    @Transactional
    public void publishCardStatusChanged(CreditCard card, String previousStatus, String reason, String changedBy) {
        CardStatusChangedEvent event = CardStatusChangedEvent.create(
                card.getCardNumber(),
                card.getAccountId(),
                previousStatus,
                card.getActiveStatus(),
                reason,
                changedBy
        );

        saveToOutbox(event);
    }

    private void saveToOutbox(CardStatusChangedEvent event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);

            CardOutbox outbox = CardOutbox.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .aggregateId(event.getAggregateId())
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);

            log.info("Card event saved to outbox: eventId={}, type={}, card={}",
                    event.getEventId(), event.getEventType(), event.getMaskedCardNumber());

        } catch (Exception e) {
            log.error("Failed to save card event to outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish card event", e);
        }
    }
}
