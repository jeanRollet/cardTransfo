package com.carddemo.partner.repository;

import com.carddemo.partner.entity.PartnerDailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerDailyUsageRepository extends JpaRepository<PartnerDailyUsage, Integer> {

    Optional<PartnerDailyUsage> findByPartnerIdAndUsageDate(Integer partnerId, LocalDate usageDate);

    List<PartnerDailyUsage> findByPartnerIdAndUsageDateBetweenOrderByUsageDateDesc(
            Integer partnerId, LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query("UPDATE PartnerDailyUsage u SET u.requestCount = u.requestCount + 1, " +
           "u.successfulCount = u.successfulCount + :successIncrement, " +
           "u.failedCount = u.failedCount + :failedIncrement " +
           "WHERE u.partnerId = :partnerId AND u.usageDate = :date")
    int incrementUsage(Integer partnerId, LocalDate date, int successIncrement, int failedIncrement);

    @Query("SELECT SUM(u.requestCount) FROM PartnerDailyUsage u WHERE u.partnerId = :partnerId " +
           "AND u.usageDate BETWEEN :startDate AND :endDate")
    Long sumRequestsInRange(Integer partnerId, LocalDate startDate, LocalDate endDate);
}
