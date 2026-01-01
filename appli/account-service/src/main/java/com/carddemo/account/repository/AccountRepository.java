package com.carddemo.account.repository;

import com.carddemo.account.entity.Account;
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
 * Repository for Account entity (ACCTDAT VSAM access)
 * Replaces CICS READ/BROWSE operations on ACCTDAT file
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Find accounts by customer ID (COACTVWC main query)
    List<Account> findByCustomerId(Integer customerId);

    // Find all active accounts
    List<Account> findByActiveStatus(String activeStatus);

    // Paginated query for all accounts
    Page<Account> findAllByOrderByAccountIdAsc(Pageable pageable);

    // Find accounts expiring within a date range
    @Query("SELECT a FROM Account a WHERE a.expiryDate BETWEEN :startDate AND :endDate")
    List<Account> findExpiringAccounts(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    // Find accounts with high utilization
    @Query("SELECT a FROM Account a WHERE a.currentBalance >= (a.creditLimit * :threshold / 100)")
    List<Account> findHighUtilizationAccounts(@Param("threshold") BigDecimal threshold);

    // Find accounts by group
    List<Account> findByGroupId(String groupId);

    // Search accounts by customer name (JOIN query)
    @Query("SELECT a FROM Account a JOIN FETCH a.customer c " +
           "WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Account> searchByCustomerName(@Param("name") String name);

    // Count accounts by status
    @Query("SELECT COUNT(a) FROM Account a WHERE a.activeStatus = :status")
    long countByStatus(@Param("status") String status);

    // Get total balance across all accounts
    @Query("SELECT COALESCE(SUM(a.currentBalance), 0) FROM Account a WHERE a.activeStatus = 'Y'")
    BigDecimal getTotalActiveBalance();

    // Get total credit limit across all accounts
    @Query("SELECT COALESCE(SUM(a.creditLimit), 0) FROM Account a WHERE a.activeStatus = 'Y'")
    BigDecimal getTotalCreditLimit();
}
