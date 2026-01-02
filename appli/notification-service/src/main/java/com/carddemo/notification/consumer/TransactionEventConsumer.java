package com.carddemo.notification.consumer;

import com.carddemo.notification.service.WebhookDeliveryService;
import com.carddemo.shared.event.KafkaTopics;
import com.carddemo.shared.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Transaction Event Consumer
 *
 * Consumes TransactionCreatedEvent from Kafka and creates webhook deliveries.
 * Replaces: CICS MQ GET for transaction messages
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final WebhookDeliveryService webhookDeliveryService;

    @KafkaListener(
            topics = KafkaTopics.TRANSACTIONS,
            groupId = KafkaTopics.NOTIFICATION_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload TransactionCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received TransactionCreatedEvent: eventId={}, transactionId={}, accountId={}, partition={}, offset={}",
                event.getEventId(),
                event.getTransactionId(),
                event.getAccountId(),
                partition,
                offset);

        try {
            int deliveries = webhookDeliveryService.createDeliveriesForEvent(event);
            log.info("Created {} webhook deliveries for transaction event {}", deliveries, event.getEventId());
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing TransactionCreatedEvent {}: {}",
                    event.getEventId(), e.getMessage(), e);
            // Don't acknowledge - will be redelivered
        }
    }
}
