package com.carddemo.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pending Authorization Entity
 *
 * Stores pending card authorizations awaiting settlement or customer review.
 * Part of COAUTH0C (Pending Authorization View) CICS transaction migration.
 */
@Entity
@Table(name = "pending_authorizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long authId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    @Column(name = "merchant_name", length = 100, nullable = false)
    private String merchantName;

    @Column(name = "merchant_category", length = 50)
    private String merchantCategory;

    @Column(name = "merchant_city", length = 50)
    private String merchantCity;

    @Column(name = "merchant_country", length = 50)
    @Builder.Default
    private String merchantCountry = "USA";

    @Column(name = "amount", precision = 11, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "auth_code", length = 10)
    private String authCode;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "decline_reason", length = 100)
    private String declineReason;

    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "is_fraud_alert", nullable = false)
    @Builder.Default
    private Boolean isFraudAlert = false;

    @Column(name = "auth_timestamp", nullable = false)
    private LocalDateTime authTimestamp;

    @Column(name = "expiry_timestamp")
    private LocalDateTime expiryTimestamp;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    public String getAmountFormatted() {
        return amount != null ? String.format("$%.2f", amount) : "$0.00";
    }

    public String getStatusName() {
        if (status == null) return "Unknown";
        return switch (status) {
            case "PENDING" -> "Pending";
            case "APPROVED" -> "Approved";
            case "DECLINED" -> "Declined";
            case "EXPIRED" -> "Expired";
            case "SETTLED" -> "Settled";
            case "REVERSED" -> "Reversed";
            default -> status;
        };
    }
}
