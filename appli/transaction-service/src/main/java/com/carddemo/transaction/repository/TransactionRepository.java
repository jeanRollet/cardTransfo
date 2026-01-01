package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Transaction Repository
 *
 * Data access layer for transactions.
 * Replaces COBOL VSAM file operations from COTRN00C.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find transactions by account ID ordered by date descending
     * Replaces COBOL READ-TRAN-FILE paragraph
     */
    List<Transaction> findByAccountIdOrderByTransactionDateDescTransactionTimeDesc(Long accountId);

    /**
     * Find transactions by account ID with pagination
     */
    Page<Transaction> findByAccountIdOrderByTransactionDateDescTransactionTimeDesc(Long accountId, Pageable pageable);

    /**
     * Find transactions by card number
     */
    List<Transaction> findByCardNumberOrderByTransactionDateDescTransactionTimeDesc(String cardNumber);

    /**
     * Find transactions by customer ID (across all their accounts)
     */
    @Query(value = "SELECT t.* FROM transactions t " +
            "INNER JOIN accounts a ON t.account_id = a.account_id " +
            "WHERE a.customer_id = :customerId " +
            "ORDER BY t.transaction_date DESC, t.transaction_time DESC",
            nativeQuery = true)
    List<Transaction> findByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Find transactions by customer ID with pagination
     */
    @Query(value = "SELECT t.* FROM transactions t " +
            "INNER JOIN accounts a ON t.account_id = a.account_id " +
            "WHERE a.customer_id = :customerId " +
            "ORDER BY t.transaction_date DESC, t.transaction_time DESC",
            countQuery = "SELECT COUNT(*) FROM transactions t " +
                    "INNER JOIN accounts a ON t.account_id = a.account_id " +
                    "WHERE a.customer_id = :customerId",
            nativeQuery = true)
    Page<Transaction> findByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);

    /**
     * Find transactions within a date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<Transaction> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find transactions by account and date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find transactions by type
     */
    Page<Transaction> findByTransactionTypeOrderByTransactionDateDescTransactionTimeDesc(
            String transactionType, Pageable pageable);

    /**
     * Search transactions by description or merchant name
     */
    @Query("SELECT t FROM Transaction t WHERE " +
            "LOWER(t.transactionDesc) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :term, '%')) " +
            "ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<Transaction> searchByTerm(@Param("term") String term);

    /**
     * Get total amount by account
     */
    @Query("SELECT COALESCE(SUM(t.transactionAmount), 0) FROM Transaction t WHERE t.accountId = :accountId")
    BigDecimal getTotalAmountByAccount(@Param("accountId") Long accountId);

    /**
     * Get total credits by account
     */
    @Query("SELECT COALESCE(SUM(t.transactionAmount), 0) FROM Transaction t " +
            "WHERE t.accountId = :accountId AND t.transactionAmount > 0")
    BigDecimal getTotalCreditsByAccount(@Param("accountId") Long accountId);

    /**
     * Get total debits by account
     */
    @Query("SELECT COALESCE(SUM(t.transactionAmount), 0) FROM Transaction t " +
            "WHERE t.accountId = :accountId AND t.transactionAmount < 0")
    BigDecimal getTotalDebitsByAccount(@Param("accountId") Long accountId);

    /**
     * Count transactions by account
     */
    long countByAccountId(Long accountId);

    /**
     * Count transactions by type
     */
    long countByTransactionType(String transactionType);

    /**
     * Get transaction counts by category for an account
     */
    @Query("SELECT t.transactionCategory, COUNT(t), SUM(t.transactionAmount) " +
            "FROM Transaction t WHERE t.accountId = :accountId " +
            "GROUP BY t.transactionCategory")
    List<Object[]> getStatsByCategory(@Param("accountId") Long accountId);

    /**
     * Get all transactions ordered by date
     */
    Page<Transaction> findAllByOrderByTransactionDateDescTransactionTimeDesc(Pageable pageable);
}
