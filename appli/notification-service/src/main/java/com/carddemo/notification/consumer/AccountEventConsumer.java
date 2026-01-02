package com.carddemo.notification.consumer;

import com.carddemo.notification.service.WebhookDeliveryService;
import com.carddemo.shared.event.AccountUpdatedEvent;
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
 * Account Event Consumer
 *
 * Consumes AccountUpdatedEvent from Kafka and creates webhook deliveries.
 * Replaces: CICS MQ GET for account update messages
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final WebhookDeliveryService webhookDeliveryService;

    @KafkaListener(
            topics = KafkaTopics.ACCOUNTS,
            groupId = KafkaTopics.NOTIFICATION_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload AccountUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received AccountUpdatedEvent: eventId={}, accountId={}, updateType={}, partition={}, offset={}",
                event.getEventId(),
                event.getAccountId(),
                event.getUpdateType(),
                partition,
                offset);

        try {
            int deliveries = webhookDeliveryService.createDeliveriesForEvent(event);
            log.info("Created {} webhook deliveries for account event {}", deliveries, event.getEventId());
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing AccountUpdatedEvent {}: {}",
                    event.getEventId(), e.getMessage(), e);
            // Don't acknowledge - will be redelivered
        }
    }
}
