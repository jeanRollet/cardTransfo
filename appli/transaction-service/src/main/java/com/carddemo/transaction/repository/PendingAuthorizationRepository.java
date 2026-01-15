package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.PendingAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PendingAuthorizationRepository extends JpaRepository<PendingAuthorization, Long> {

    List<PendingAuthorization> findByStatusOrderByAuthTimestampDesc(String status);

    List<PendingAuthorization> findByAccountIdAndStatusOrderByAuthTimestampDesc(Long accountId, String status);

    List<PendingAuthorization> findByIsFraudAlertTrueAndStatusOrderByAuthTimestampDesc(String status);

    @Query("SELECT COUNT(pa) FROM PendingAuthorization pa WHERE pa.status = 'PENDING'")
    long countPending();

    @Query("SELECT COALESCE(SUM(pa.amount), 0) FROM PendingAuthorization pa WHERE pa.status = 'PENDING'")
    BigDecimal sumPendingAmount();

    @Query("SELECT COUNT(pa) FROM PendingAuthorization pa WHERE pa.isFraudAlert = true AND pa.status = 'PENDING'")
    long countFraudAlerts();

    @Query("SELECT COUNT(pa) FROM PendingAuthorization pa WHERE pa.riskScore >= 70 AND pa.status = 'PENDING'")
    long countHighRisk();

    // By account ID for customer filtering
    @Query("SELECT COUNT(pa) FROM PendingAuthorization pa WHERE pa.accountId = :accountId AND pa.status = 'PENDING'")
    long countPendingByAccount(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(pa.amount), 0) FROM PendingAuthorization pa WHERE pa.accountId = :accountId AND pa.status = 'PENDING'")
    BigDecimal sumPendingAmountByAccount(@Param("accountId") Long accountId);
}
