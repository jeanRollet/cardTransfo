package com.carddemo.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 *
 * Replaces MQ Series message structures from the mainframe.
 * Each event follows the CloudEvents specification.
 *
 * @see <a href="https://cloudevents.io/">CloudEvents Specification</a>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TransactionCreatedEvent.class, name = "TransactionCreated"),
    @JsonSubTypes.Type(value = CardStatusChangedEvent.class, name = "CardStatusChanged"),
    @JsonSubTypes.Type(value = AccountUpdatedEvent.class, name = "AccountUpdated")
})
public abstract class DomainEvent {

    /**
     * Unique identifier for this event instance
     */
    private UUID eventId;

    /**
     * Type of event (e.g., TransactionCreated, CardStatusChanged)
     */
    private String eventType;

    /**
     * Schema version for event evolution
     */
    private String schemaVersion;

    /**
     * Source service that generated the event
     */
    private String source;

    /**
     * Aggregate ID (e.g., accountId, cardNumber)
     */
    private String aggregateId;

    /**
     * When the event occurred
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;

    /**
     * Initialize common event fields
     */
    protected void initializeEventFields(String eventType, String source, String aggregateId) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.schemaVersion = "1.0";
        this.source = source;
        this.aggregateId = aggregateId;
        this.occurredAt = Instant.now();
    }

    /**
     * Get the required scope to receive this event via webhook
     */
    public abstract String getRequiredScope();

    /**
     * Get the Kafka topic for this event type
     */
    public abstract String getTopic();
}
