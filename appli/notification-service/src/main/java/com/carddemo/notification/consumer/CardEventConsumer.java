package com.carddemo.notification.consumer;

import com.carddemo.notification.service.WebhookDeliveryService;
import com.carddemo.shared.event.CardStatusChangedEvent;
import com.carddemo.shared.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Card Event Consumer
 *
 * Consumes CardStatusChangedEvent from Kafka and creates webhook deliveries.
 * Replaces: CICS MQ GET for card status messages
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CardEventConsumer {

    private final WebhookDeliveryService webhookDeliveryService;

    @KafkaListener(
            topics = KafkaTopics.CARDS,
            groupId = KafkaTopics.NOTIFICATION_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload CardStatusChangedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received CardStatusChangedEvent: eventId={}, card={}, status={}->{}, partition={}, offset={}",
                event.getEventId(),
                event.getMaskedCardNumber(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                partition,
                offset);

        try {
            int deliveries = webhookDeliveryService.createDeliveriesForEvent(event);
            log.info("Created {} webhook deliveries for card event {}", deliveries, event.getEventId());
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing CardStatusChangedEvent {}: {}",
                    event.getEventId(), e.getMessage(), e);
            // Don't acknowledge - will be redelivered
        }
    }
}
