package com.carddemo.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Transaction List Response DTO
 *
 * Response object containing list of transactions with summary statistics.
 * Used for account transaction history view.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionListResponse {

    private Long accountId;
    private List<TransactionResponse> transactions;
    private int totalTransactions;
    private int creditCount;
    private int debitCount;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private BigDecimal netAmount;
    private String totalCreditsFormatted;
    private String totalDebitsFormatted;
    private String netAmountFormatted;

    /**
     * Create response with calculated statistics
     */
    public static TransactionListResponse of(Long accountId, List<TransactionResponse> transactions) {
        int creditCount = 0;
        int debitCount = 0;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;

        for (TransactionResponse t : transactions) {
            if (t.isCredit()) {
                creditCount++;
                totalCredits = totalCredits.add(t.getTransactionAmount());
            } else if (t.isDebit()) {
                debitCount++;
                totalDebits = totalDebits.add(t.getTransactionAmount().abs());
            }
        }

        BigDecimal netAmount = totalCredits.subtract(totalDebits);

        return TransactionListResponse.builder()
                .accountId(accountId)
                .transactions(transactions)
                .totalTransactions(transactions.size())
                .creditCount(creditCount)
                .debitCount(debitCount)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .netAmount(netAmount)
                .totalCreditsFormatted("+$" + totalCredits.setScale(2).toPlainString())
                .totalDebitsFormatted("-$" + totalDebits.setScale(2).toPlainString())
                .netAmountFormatted(formatNetAmount(netAmount))
                .build();
    }

    private static String formatNetAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            return "+$" + amount.setScale(2).toPlainString();
        } else {
            return "-$" + amount.abs().setScale(2).toPlainString();
        }
    }
}
