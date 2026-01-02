package com.carddemo.shared.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event emitted when an account is updated.
 *
 * Replaces: MQ message from COACTVWC (Account Management)
 * Topic: carddemo.accounts
 * Required Scope: accounts:read
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccountUpdatedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "AccountUpdated";
    public static final String TOPIC = "carddemo.accounts";
    public static final String REQUIRED_SCOPE = "accounts:read";

    private Long accountId;
    private Integer customerId;
    private String updateType;
    private String previousStatus;
    private String newStatus;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private BigDecimal previousCreditLimit;
    private BigDecimal newCreditLimit;
    private String changedBy;

    /**
     * Update types
     */
    public static final String UPDATE_TYPE_STATUS = "STATUS_CHANGE";
    public static final String UPDATE_TYPE_BALANCE = "BALANCE_CHANGE";
    public static final String UPDATE_TYPE_LIMIT = "LIMIT_CHANGE";
    public static final String UPDATE_TYPE_GENERAL = "GENERAL_UPDATE";

    /**
     * Factory method for status change
     */
    public static AccountUpdatedEvent createStatusChange(
            Long accountId,
            Integer customerId,
            String previousStatus,
            String newStatus,
            String changedBy) {

        AccountUpdatedEvent event = AccountUpdatedEvent.builder()
                .accountId(accountId)
                .customerId(customerId)
                .updateType(UPDATE_TYPE_STATUS)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .build();

        event.initializeEventFields(EVENT_TYPE, "account-service", String.valueOf(accountId));
        return event;
    }

    /**
     * Factory method for balance change
     */
    public static AccountUpdatedEvent createBalanceChange(
            Long accountId,
            Integer customerId,
            BigDecimal previousBalance,
            BigDecimal newBalance,
            String changedBy) {

        AccountUpdatedEvent event = AccountUpdatedEvent.builder()
                .accountId(accountId)
                .customerId(customerId)
                .updateType(UPDATE_TYPE_BALANCE)
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .changedBy(changedBy)
                .build();

        event.initializeEventFields(EVENT_TYPE, "account-service", String.valueOf(accountId));
        return event;
    }

    /**
     * Factory method for credit limit change
     */
    public static AccountUpdatedEvent createLimitChange(
            Long accountId,
            Integer customerId,
            BigDecimal previousCreditLimit,
            BigDecimal newCreditLimit,
            String changedBy) {

        AccountUpdatedEvent event = AccountUpdatedEvent.builder()
                .accountId(accountId)
                .customerId(customerId)
                .updateType(UPDATE_TYPE_LIMIT)
                .previousCreditLimit(previousCreditLimit)
                .newCreditLimit(newCreditLimit)
                .changedBy(changedBy)
                .build();

        event.initializeEventFields(EVENT_TYPE, "account-service", String.valueOf(accountId));
        return event;
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
        if (status == null) return null;
        return switch (status) {
            case "Y" -> "Active";
            case "N" -> "Closed";
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
