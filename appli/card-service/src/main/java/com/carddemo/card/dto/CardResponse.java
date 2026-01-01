package com.carddemo.card.dto;

import com.carddemo.card.entity.CreditCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Card Response DTO
 *
 * Used for API responses. Card number is masked for security.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponse {

    private String cardNumber;          // Masked: **** **** **** 1234
    private String lastFourDigits;      // Last 4 digits only
    private Long accountId;
    private String cardType;            // VC, MC, AX, DC
    private String cardTypeName;        // Visa, Mastercard, etc.
    private String embossedName;
    private LocalDate expiryDate;
    private String expiryFormatted;     // MM/YY format
    private String status;              // Y, N, S
    private String statusName;          // Active, Closed, Blocked
    private LocalDate issuedDate;
    private boolean isExpired;
    private boolean isActive;

    /**
     * Create from entity with masked card number
     */
    public static CardResponse fromEntity(CreditCard card) {
        return CardResponse.builder()
                .cardNumber(card.getMaskedCardNumber())
                .lastFourDigits(card.getCardNumber().substring(card.getCardNumber().length() - 4))
                .accountId(card.getAccountId())
                .cardType(card.getCardType())
                .cardTypeName(card.getCardTypeDisplayName())
                .embossedName(card.getEmbossedName())
                .expiryDate(card.getExpiryDate())
                .expiryFormatted(formatExpiry(card.getExpiryDate()))
                .status(card.getActiveStatus())
                .statusName(card.getStatusDisplayName())
                .issuedDate(card.getIssuedDate())
                .isExpired(card.isExpired())
                .isActive(card.isActive())
                .build();
    }

    private static String formatExpiry(LocalDate date) {
        if (date == null) return "";
        return String.format("%02d/%02d", date.getMonthValue(), date.getYear() % 100);
    }
}
