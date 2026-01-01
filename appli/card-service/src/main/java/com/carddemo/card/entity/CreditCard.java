package com.carddemo.card.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Credit Card Entity
 *
 * Maps to PostgreSQL 'credit_cards' table (from CARDDAT VSAM file).
 *
 * COBOL Copybook Mapping (CARD-RECORD):
 *   CARD-NUM        PIC X(16)  -> cardNumber
 *   CARD-ACCT-ID    PIC 9(11)  -> accountId
 *   CARD-TYPE       PIC X(2)   -> cardType (VC/MC/AX/DC)
 *   CARD-NAME       PIC X(50)  -> embossedName
 *   CARD-EXPIRY     PIC 9(8)   -> expiryDate
 *   CARD-STATUS     PIC X(1)   -> activeStatus
 *
 * CICS Transaction Mapping:
 *   COCRDLIC -> List cards for account
 *   COCRDSLC -> Select/view card details
 *   COCRDUPC -> Update card status
 */
@Entity
@Table(name = "credit_cards")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCard {

    /**
     * Card Number - Primary Key (16 digits)
     */
    @Id
    @Column(name = "card_number", length = 16, nullable = false)
    private String cardNumber;

    /**
     * Account ID (11 digits, foreign key to accounts)
     */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /**
     * Card Type: VC=Visa, MC=Mastercard, AX=Amex, DC=Discover
     */
    @Column(name = "card_type", columnDefinition = "char(2)", nullable = false)
    private String cardType;

    /**
     * Name embossed on card
     */
    @Column(name = "embossed_name", length = 50, nullable = false)
    private String embossedName;

    /**
     * Card expiry date
     */
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    /**
     * Hashed CVV (for security)
     */
    @Column(name = "cvv_hash", length = 60)
    private String cvvHash;

    /**
     * Active Status: Y=Active, N=Closed, S=Stolen/Blocked
     */
    @Column(name = "active_status", columnDefinition = "char(1)", nullable = false)
    @Builder.Default
    private String activeStatus = "Y";

    /**
     * Card issue date
     */
    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    /**
     * Record creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business methods

    /**
     * Check if card is active
     */
    public boolean isActive() {
        return "Y".equals(activeStatus);
    }

    /**
     * Check if card is expired
     */
    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Check if card is blocked (stolen)
     */
    public boolean isBlocked() {
        return "S".equals(activeStatus);
    }

    /**
     * Get masked card number (show only last 4 digits)
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Get card type display name
     */
    public String getCardTypeDisplayName() {
        return switch (cardType) {
            case "VC" -> "Visa";
            case "MC" -> "Mastercard";
            case "AX" -> "American Express";
            case "DC" -> "Discover";
            default -> cardType;
        };
    }

    /**
     * Get status display name
     */
    public String getStatusDisplayName() {
        return switch (activeStatus) {
            case "Y" -> "Active";
            case "N" -> "Closed";
            case "S" -> "Blocked";
            default -> activeStatus;
        };
    }
}
