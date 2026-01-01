package com.carddemo.transaction.dto;

import com.carddemo.transaction.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Transaction Response DTO
 *
 * Response object for single transaction details.
 * Includes formatted fields for display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long transactionId;
    private Long accountId;
    private String transactionType;
    private String transactionTypeName;
    private String transactionCategory;
    private String categoryName;
    private String transactionSource;
    private String sourceName;
    private String transactionDesc;
    private BigDecimal transactionAmount;
    private String amountFormatted;
    private String merchantId;
    private String merchantName;
    private String merchantCity;
    private String merchantZip;
    private String merchantLocation;
    private String cardNumber;
    private String originalTranId;
    private String transactionDate;
    private String transactionTime;
    private String transactionDateTime;
    private boolean isDebit;
    private boolean isCredit;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Create response from entity
     */
    public static TransactionResponse fromEntity(Transaction transaction) {
        String merchantLocation = buildMerchantLocation(
                transaction.getMerchantCity(),
                transaction.getMerchantZip()
        );

        String amountFormatted = formatAmount(transaction.getTransactionAmount());

        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .accountId(transaction.getAccountId())
                .transactionType(transaction.getTransactionType())
                .transactionTypeName(transaction.getTransactionTypeDisplayName())
                .transactionCategory(transaction.getTransactionCategory())
                .categoryName(transaction.getCategoryDisplayName())
                .transactionSource(transaction.getTransactionSource())
                .sourceName(transaction.getSourceDisplayName())
                .transactionDesc(transaction.getTransactionDesc())
                .transactionAmount(transaction.getTransactionAmount())
                .amountFormatted(amountFormatted)
                .merchantId(transaction.getMerchantId())
                .merchantName(transaction.getMerchantName())
                .merchantCity(transaction.getMerchantCity())
                .merchantZip(transaction.getMerchantZip())
                .merchantLocation(merchantLocation)
                .cardNumber(transaction.getMaskedCardNumber())
                .originalTranId(transaction.getOriginalTranId())
                .transactionDate(transaction.getTransactionDate().format(DATE_FORMATTER))
                .transactionTime(transaction.getTransactionTime().format(TIME_FORMATTER))
                .transactionDateTime(transaction.getTransactionDate().format(DATE_FORMATTER) +
                        " " + transaction.getTransactionTime().format(TIME_FORMATTER))
                .isDebit(transaction.isDebit())
                .isCredit(transaction.isCredit())
                .build();
    }

    private static String buildMerchantLocation(String city, String zip) {
        if (city == null && zip == null) return null;
        if (city == null) return zip;
        if (zip == null) return city;
        return city + ", " + zip;
    }

    private static String formatAmount(BigDecimal amount) {
        if (amount == null) return "$0.00";
        String prefix = amount.compareTo(BigDecimal.ZERO) >= 0 ? "+$" : "-$";
        return prefix + amount.abs().setScale(2).toPlainString();
    }
}
