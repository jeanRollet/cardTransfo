package com.carddemo.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationStatsResponse {
    private Long totalPending;
    private BigDecimal totalAmount;
    private String totalAmountFormatted;
    private Long fraudAlerts;
    private Long highRiskCount;
}
