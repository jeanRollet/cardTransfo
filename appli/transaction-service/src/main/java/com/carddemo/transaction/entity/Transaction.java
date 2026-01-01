package com.carddemo.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Transaction Entity
 *
 * Maps to the transactions table in PostgreSQL.
 * Replaces COBOL TRAN-RECORD copybook from CVTRA00Y.cpy
 *
 * COBOL Field Mapping:
 *   TRAN-ID          -> transactionId
 *   TRAN-ACCT-ID     -> accountId
 *   TRAN-TYPE-CD     -> transactionType
 *   TRAN-CAT-CD      -> transactionCategory
 *   TRAN-SOURCE      -> transactionSource
 *   TRAN-DESC        -> transactionDesc
 *   TRAN-AMT         -> transactionAmount
 *   TRAN-MERCHANT-ID -> merchantId
 *   TRAN-MERCHANT-NAME -> merchantName
 *   TRAN-MERCHANT-CITY -> merchantCity
 *   TRAN-MERCHANT-ZIP  -> merchantZip
 *   TRAN-CARD-NUM    -> cardNumber
 *   TRAN-ORIG-ID     -> originalTranId
 *   TRAN-DATE        -> transactionDate
 *   TRAN-TIME        -> transactionTime
 */
@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "transaction_type", length = 4, nullable = false)
    private String transactionType;

    @Column(name = "transaction_category", length = 10)
    private String transactionCategory;

    @Column(name = "transaction_source", length = 10)
    private String transactionSource;

    @Column(name = "transaction_desc", length = 100)
    private String transactionDesc;

    @Column(name = "transaction_amount", precision = 11, scale = 2, nullable = false)
    private BigDecimal transactionAmount;

    @Column(name = "merchant_id", length = 16)
    private String merchantId;

    @Column(name = "merchant_name", length = 50)
    private String merchantName;

    @Column(name = "merchant_city", length = 30)
    private String merchantCity;

    @Column(name = "merchant_zip", length = 10)
    private String merchantZip;

    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @Column(name = "original_tranid", length = 16)
    private String originalTranId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Business methods

    /**
     * Check if this is a debit transaction (money going out)
     */
    public boolean isDebit() {
        return transactionAmount != null && transactionAmount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Check if this is a credit transaction (money coming in)
     */
    public boolean isCredit() {
        return transactionAmount != null && transactionAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get masked card number for display
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Get transaction type display name
     */
    public String getTransactionTypeDisplayName() {
        if (transactionType == null) return "Unknown";
        return switch (transactionType) {
            case "SALE" -> "Purchase";
            case "PYMT" -> "Payment";
            case "RFND" -> "Refund";
            case "CASH" -> "Cash Advance";
            case "FEE" -> "Fee";
            case "INT" -> "Interest";
            default -> transactionType;
        };
    }

    /**
     * Get category display name
     */
    public String getCategoryDisplayName() {
        if (transactionCategory == null) return "Other";
        return switch (transactionCategory) {
            case "GROCERY" -> "Grocery";
            case "GAS" -> "Gas & Fuel";
            case "RETAIL" -> "Retail";
            case "DINING" -> "Dining";
            case "TRAVEL" -> "Travel";
            case "PAYMENT" -> "Payment";
            case "ONLINE" -> "Online Shopping";
            default -> transactionCategory;
        };
    }

    /**
     * Get source display name
     */
    public String getSourceDisplayName() {
        if (transactionSource == null) return "Other";
        return switch (transactionSource) {
            case "POS" -> "Point of Sale";
            case "ONLINE" -> "Online";
            case "AUTOPAY" -> "Auto Pay";
            case "ATM" -> "ATM";
            case "MOBILE" -> "Mobile";
            default -> transactionSource;
        };
    }
}
