package com.carddemo.transaction.dto;

import com.carddemo.transaction.entity.PendingAuthorization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingAuthorizationResponse {
    private Long authId;
    private Long accountId;
    private String cardNumber;
    private String maskedCardNumber;
    private String merchantName;
    private String merchantCategory;
    private String merchantCity;
    private String merchantCountry;
    private BigDecimal amount;
    private String amountFormatted;
    private String currency;
    private String authCode;
    private String status;
    private String statusName;
    private String declineReason;
    private Integer riskScore;
    private Boolean isFraudAlert;
    private LocalDateTime authTimestamp;
    private LocalDateTime expiryTimestamp;
    private LocalDateTime settledAt;

    public static PendingAuthorizationResponse fromEntity(PendingAuthorization entity) {
        return PendingAuthorizationResponse.builder()
                .authId(entity.getAuthId())
                .accountId(entity.getAccountId())
                .cardNumber(entity.getCardNumber())
                .maskedCardNumber(entity.getMaskedCardNumber())
                .merchantName(entity.getMerchantName())
                .merchantCategory(entity.getMerchantCategory())
                .merchantCity(entity.getMerchantCity())
                .merchantCountry(entity.getMerchantCountry())
                .amount(entity.getAmount())
                .amountFormatted(entity.getAmountFormatted())
                .currency(entity.getCurrency())
                .authCode(entity.getAuthCode())
                .status(entity.getStatus())
                .statusName(entity.getStatusName())
                .declineReason(entity.getDeclineReason())
                .riskScore(entity.getRiskScore())
                .isFraudAlert(entity.getIsFraudAlert())
                .authTimestamp(entity.getAuthTimestamp())
                .expiryTimestamp(entity.getExpiryTimestamp())
                .settledAt(entity.getSettledAt())
                .build();
    }
}
