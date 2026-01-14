package com.carddemo.card.repository;

import com.carddemo.card.entity.CreditCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Credit Card Repository
 *
 * Provides data access for credit card operations.
 * Replaces VSAM CARDDAT file access patterns from COBOL.
 */
@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, String> {

    /**
     * Find all cards for an account (COCRDLIC - Card List)
     */
    List<CreditCard> findByAccountIdOrderByIssuedDateDesc(Long accountId);

    /**
     * Find all cards for an account with pagination
     */
    Page<CreditCard> findByAccountId(Long accountId, Pageable pageable);

    /**
     * Find active cards for an account
     */
    List<CreditCard> findByAccountIdAndActiveStatus(Long accountId, String activeStatus);

    /**
     * Find cards by type
     */
    List<CreditCard> findByCardType(String cardType);

    /**
     * Find cards expiring before a date
     */
    List<CreditCard> findByExpiryDateBeforeAndActiveStatus(LocalDate date, String activeStatus);

    /**
     * Find cards expiring within a period (for renewal notifications)
     */
    @Query("SELECT c FROM CreditCard c WHERE c.expiryDate BETWEEN :startDate AND :endDate AND c.activeStatus = 'Y'")
    List<CreditCard> findCardsExpiringBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count active cards for an account
     */
    long countByAccountIdAndActiveStatus(Long accountId, String activeStatus);

    /**
     * Check if card exists for account
     */
    boolean existsByCardNumberAndAccountId(String cardNumber, Long accountId);

    /**
     * Find card with full details (for COCRDSLC - Card Select)
     */
    @Query("SELECT c FROM CreditCard c WHERE c.cardNumber = :cardNumber")
    Optional<CreditCard> findCardDetails(@Param("cardNumber") String cardNumber);

    /**
     * Find all cards by status with pagination
     */
    Page<CreditCard> findByActiveStatus(String activeStatus, Pageable pageable);

    /**
     * Search cards by embossed name (partial match)
     */
    @Query("SELECT c FROM CreditCard c WHERE UPPER(c.embossedName) LIKE UPPER(CONCAT('%', :name, '%'))")
    List<CreditCard> searchByEmbossedName(@Param("name") String name);

    /**
     * Find all cards for a customer (across all their accounts)
     */
    @Query(value = "SELECT cc.* FROM credit_cards cc " +
            "INNER JOIN accounts a ON cc.account_id = a.account_id " +
            "WHERE a.customer_id = :customerId " +
            "ORDER BY cc.issued_date DESC", nativeQuery = true)
    List<CreditCard> findByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Find card by last 4 digits (for frontend navigation)
     * Note: In production, this should be combined with additional identifiers for security
     */
    @Query("SELECT c FROM CreditCard c WHERE c.cardNumber LIKE CONCAT('%', :lastFour)")
    List<CreditCard> findByLastFourDigits(@Param("lastFour") String lastFour);
}
