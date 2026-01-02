package com.carddemo.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Event emitted when a new transaction is created.
 *
 * Replaces: MQ message from COTRN00C (Transaction Processing)
 * Topic: carddemo.transactions
 * Required Scope: transactions:read
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransactionCreatedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "TransactionCreated";
    public static final String TOPIC = "carddemo.transactions";
    public static final String REQUIRED_SCOPE = "transactions:read";

    private Long transactionId;
    private Long accountId;
    private String transactionType;
    private String transactionCategory;
    private String transactionSource;
    private String transactionDesc;
    private BigDecimal transactionAmount;
    private String merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    private String maskedCardNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate transactionDate;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime transactionTime;

    /**
     * Factory method to create a TransactionCreatedEvent
     */
    public static TransactionCreatedEvent create(
            Long transactionId,
            Long accountId,
            String transactionType,
            String transactionCategory,
            String transactionSource,
            String transactionDesc,
            BigDecimal transactionAmount,
            String merchantId,
            String merchantName,
            String merchantCity,
            String merchantZip,
            String cardNumber,
            LocalDate transactionDate,
            LocalTime transactionTime) {

        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(transactionId)
                .accountId(accountId)
                .transactionType(transactionType)
                .transactionCategory(transactionCategory)
                .transactionSource(transactionSource)
                .transactionDesc(transactionDesc)
                .transactionAmount(transactionAmount)
                .merchantId(merchantId)
                .merchantName(merchantName)
                .merchantCity(merchantCity)
                .merchantZip(merchantZip)
                .maskedCardNumber(maskCardNumber(cardNumber))
                .transactionDate(transactionDate)
                .transactionTime(transactionTime)
                .build();

        event.initializeEventFields(EVENT_TYPE, "transaction-service", String.valueOf(accountId));
        return event;
    }

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
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
