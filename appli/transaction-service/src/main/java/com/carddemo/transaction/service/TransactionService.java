package com.carddemo.transaction.service;

import com.carddemo.transaction.dto.TransactionListResponse;
import com.carddemo.transaction.dto.TransactionResponse;
import com.carddemo.transaction.dto.TransactionSummaryResponse;
import com.carddemo.transaction.entity.Transaction;
import com.carddemo.transaction.exception.TransactionException;
import com.carddemo.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction Service
 *
 * Business logic for transaction operations.
 * Replaces COBOL paragraphs from COTRN00C.
 *
 * COBOL to Java Mapping:
 *   PROCESS-TRAN-LIST   -> getTransactionsByAccount()
 *   PROCESS-TRAN-SEARCH -> searchTransactions()
 *   CALC-TRAN-TOTALS    -> getTransactionSummary()
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Get all transactions for an account (COTRN00C - Transaction List)
     */
    public TransactionListResponse getTransactionsByAccount(Long accountId) {
        log.info("Fetching transactions for account: {}", accountId);

        List<Transaction> transactions = transactionRepository
                .findByAccountIdOrderByTransactionDateDescTransactionTimeDesc(accountId);

        List<TransactionResponse> responses = transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        return TransactionListResponse.of(accountId, responses);
    }

    /**
     * Get transactions for a customer (across all accounts) - for RBAC
     */
    public List<TransactionResponse> getTransactionsByCustomer(Integer customerId) {
        log.info("Fetching transactions for customer: {}", customerId);

        List<Transaction> transactions = transactionRepository.findByCustomerId(customerId);

        if (transactions.isEmpty()) {
            log.warn("No transactions found for customer: {}", customerId);
        }

        return transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions for a customer with pagination
     */
    public Page<TransactionResponse> getTransactionsByCustomer(Integer customerId, Pageable pageable) {
        log.info("Fetching transactions for customer: {} with pagination", customerId);

        return transactionRepository.findByCustomerId(customerId, pageable)
                .map(TransactionResponse::fromEntity);
    }

    /**
     * Get all transactions with pagination (Admin view)
     */
    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        log.info("Fetching all transactions with pagination");

        return transactionRepository.findAllByOrderByTransactionDateDescTransactionTimeDesc(pageable)
                .map(TransactionResponse::fromEntity);
    }

    /**
     * Get transaction details by ID
     */
    public TransactionResponse getTransactionById(Long transactionId) {
        log.info("Fetching transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> TransactionException.transactionNotFound(transactionId));

        return TransactionResponse.fromEntity(transaction);
    }

    /**
     * Get transactions by date range
     */
    public List<TransactionResponse> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching transactions between {} and {}", startDate, endDate);

        return transactionRepository.findByDateRange(startDate, endDate).stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions by account and date range
     */
    public List<TransactionResponse> getTransactionsByAccountAndDateRange(
            Long accountId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching transactions for account {} between {} and {}", accountId, startDate, endDate);

        return transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate).stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search transactions by term
     */
    public List<TransactionResponse> searchTransactions(String term) {
        log.info("Searching transactions with term: {}", term);

        return transactionRepository.searchByTerm(term).stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get transaction summary for reports (CORPT00C equivalent)
     */
    public TransactionSummaryResponse getTransactionSummary(Long accountId) {
        log.info("Generating transaction summary for account: {}", accountId);

        List<Transaction> transactions = transactionRepository
                .findByAccountIdOrderByTransactionDateDescTransactionTimeDesc(accountId);

        // Calculate statistics
        long totalTransactions = transactions.size();
        long purchaseCount = 0;
        long paymentCount = 0;
        long refundCount = 0;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            switch (t.getTransactionType()) {
                case "SALE" -> {
                    purchaseCount++;
                    totalPurchases = totalPurchases.add(t.getTransactionAmount().abs());
                }
                case "PYMT" -> {
                    paymentCount++;
                    totalPayments = totalPayments.add(t.getTransactionAmount());
                }
                case "RFND" -> {
                    refundCount++;
                    totalRefunds = totalRefunds.add(t.getTransactionAmount());
                }
            }
        }

        BigDecimal totalAmount = totalPayments.subtract(totalPurchases).add(totalRefunds);
        BigDecimal averageTransaction = totalTransactions > 0
                ? totalPurchases.divide(BigDecimal.valueOf(purchaseCount > 0 ? purchaseCount : 1), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get category breakdown
        List<Object[]> categoryStats = transactionRepository.getStatsByCategory(accountId);
        List<TransactionSummaryResponse.CategorySummary> categoryBreakdown = new ArrayList<>();

        for (Object[] row : categoryStats) {
            String category = (String) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal amount = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;

            categoryBreakdown.add(TransactionSummaryResponse.CategorySummary.builder()
                    .category(category)
                    .categoryName(getCategoryDisplayName(category))
                    .transactionCount(count)
                    .totalAmount(amount.abs())
                    .totalAmountFormatted(TransactionSummaryResponse.formatAmount(amount.abs()))
                    .percentage(totalTransactions > 0 ? (count * 100.0 / totalTransactions) : 0)
                    .build());
        }

        return TransactionSummaryResponse.builder()
                .accountId(accountId)
                .totalTransactions(totalTransactions)
                .purchaseCount(purchaseCount)
                .paymentCount(paymentCount)
                .refundCount(refundCount)
                .totalAmount(totalAmount)
                .totalPurchases(totalPurchases)
                .totalPayments(totalPayments)
                .totalRefunds(totalRefunds)
                .averageTransaction(averageTransaction)
                .totalAmountFormatted(TransactionSummaryResponse.formatAmount(totalAmount))
                .totalPurchasesFormatted(TransactionSummaryResponse.formatAmount(totalPurchases))
                .totalPaymentsFormatted(TransactionSummaryResponse.formatAmount(totalPayments))
                .averageTransactionFormatted(TransactionSummaryResponse.formatAmount(averageTransaction))
                .categoryBreakdown(categoryBreakdown)
                .build();
    }

    /**
     * Get overall summary for all transactions (Admin reports)
     */
    public TransactionSummaryResponse getOverallSummary() {
        log.info("Generating overall transaction summary");

        List<Transaction> transactions = transactionRepository.findAll();

        long totalTransactions = transactions.size();
        long purchaseCount = 0;
        long paymentCount = 0;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if ("SALE".equals(t.getTransactionType())) {
                purchaseCount++;
                totalPurchases = totalPurchases.add(t.getTransactionAmount().abs());
            } else if ("PYMT".equals(t.getTransactionType())) {
                paymentCount++;
                totalPayments = totalPayments.add(t.getTransactionAmount());
            }
        }

        BigDecimal totalAmount = totalPayments.subtract(totalPurchases);
        BigDecimal averageTransaction = purchaseCount > 0
                ? totalPurchases.divide(BigDecimal.valueOf(purchaseCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return TransactionSummaryResponse.builder()
                .totalTransactions(totalTransactions)
                .purchaseCount(purchaseCount)
                .paymentCount(paymentCount)
                .totalAmount(totalAmount)
                .totalPurchases(totalPurchases)
                .totalPayments(totalPayments)
                .averageTransaction(averageTransaction)
                .totalAmountFormatted(TransactionSummaryResponse.formatAmount(totalAmount))
                .totalPurchasesFormatted(TransactionSummaryResponse.formatAmount(totalPurchases))
                .totalPaymentsFormatted(TransactionSummaryResponse.formatAmount(totalPayments))
                .averageTransactionFormatted(TransactionSummaryResponse.formatAmount(averageTransaction))
                .build();
    }

    private String getCategoryDisplayName(String category) {
        if (category == null) return "Other";
        return switch (category) {
            case "GROCERY" -> "Grocery";
            case "GAS" -> "Gas & Fuel";
            case "RETAIL" -> "Retail";
            case "DINING" -> "Dining";
            case "TRAVEL" -> "Travel";
            case "PAYMENT" -> "Payment";
            case "ONLINE" -> "Online Shopping";
            default -> category;
        };
    }
}
