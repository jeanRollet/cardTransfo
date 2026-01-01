package com.carddemo.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Transaction Summary Response DTO
 *
 * Aggregated statistics for reporting.
 * Replaces CICS CORPT00C report transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummaryResponse {

    private Long accountId;
    private Integer customerId;

    // Transaction counts
    private long totalTransactions;
    private long purchaseCount;
    private long paymentCount;
    private long refundCount;

    // Amounts
    private BigDecimal totalAmount;
    private BigDecimal totalPurchases;
    private BigDecimal totalPayments;
    private BigDecimal totalRefunds;
    private BigDecimal averageTransaction;

    // Formatted amounts
    private String totalAmountFormatted;
    private String totalPurchasesFormatted;
    private String totalPaymentsFormatted;
    private String averageTransactionFormatted;

    // Category breakdown
    private List<CategorySummary> categoryBreakdown;

    // Monthly trend (last 6 months)
    private List<MonthlyTrend> monthlyTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySummary {
        private String category;
        private String categoryName;
        private long transactionCount;
        private BigDecimal totalAmount;
        private String totalAmountFormatted;
        private double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyTrend {
        private String month;
        private String monthName;
        private long transactionCount;
        private BigDecimal totalSpending;
        private BigDecimal totalPayments;
        private String totalSpendingFormatted;
    }

    /**
     * Format amount for display
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) return "$0.00";
        String prefix = amount.compareTo(BigDecimal.ZERO) >= 0 ? "$" : "-$";
        return prefix + amount.abs().setScale(2).toPlainString();
    }
}
