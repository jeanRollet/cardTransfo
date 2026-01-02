package com.carddemo.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event emitted when a card's status changes.
 *
 * Replaces: MQ message from COCRDLIC (Card Management)
 * Topic: carddemo.cards
 * Required Scope: cards:read
 *
 * Status values:
 *   Y = Active
 *   N = Closed
 *   S = Stolen/Blocked
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CardStatusChangedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "CardStatusChanged";
    public static final String TOPIC = "carddemo.cards";
    public static final String REQUIRED_SCOPE = "cards:read";

    private String maskedCardNumber;
    private Long accountId;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private String changedBy;

    /**
     * Factory method to create a CardStatusChangedEvent
     */
    public static CardStatusChangedEvent create(
            String cardNumber,
            Long accountId,
            String previousStatus,
            String newStatus,
            String reason,
            String changedBy) {

        CardStatusChangedEvent event = CardStatusChangedEvent.builder()
                .maskedCardNumber(maskCardNumber(cardNumber))
                .accountId(accountId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(changedBy)
                .build();

        event.initializeEventFields(EVENT_TYPE, "card-service", cardNumber);
        return event;
    }

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Get human-readable status name
     */
    public String getNewStatusDisplayName() {
        return getStatusDisplayName(newStatus);
    }

    public String getPreviousStatusDisplayName() {
        return getStatusDisplayName(previousStatus);
    }

    private static String getStatusDisplayName(String status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case "Y" -> "Active";
            case "N" -> "Closed";
            case "S" -> "Blocked/Stolen";
            default -> status;
        };
    }

    @Override
    public String getRequiredScope() {
        return REQUIRED_SCOPE;
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }
}
