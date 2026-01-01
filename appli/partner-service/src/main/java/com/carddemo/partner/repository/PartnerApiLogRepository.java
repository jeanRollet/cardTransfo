package com.carddemo.partner.repository;

import com.carddemo.partner.entity.PartnerApiLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PartnerApiLogRepository extends JpaRepository<PartnerApiLog, Long> {

    Page<PartnerApiLog> findByPartnerIdOrderByRequestTimestampDesc(Integer partnerId, Pageable pageable);

    @Query("SELECT l FROM PartnerApiLog l WHERE l.partnerId = :partnerId " +
           "AND l.requestTimestamp BETWEEN :start AND :end ORDER BY l.requestTimestamp DESC")
    List<PartnerApiLog> findByPartnerIdAndDateRange(Integer partnerId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(l) FROM PartnerApiLog l WHERE l.partnerId = :partnerId " +
           "AND l.requestTimestamp >= :since")
    long countRequestsSince(Integer partnerId, LocalDateTime since);

    @Query("SELECT AVG(l.responseTimeMs) FROM PartnerApiLog l WHERE l.partnerId = :partnerId " +
           "AND l.requestTimestamp >= :since")
    Double avgResponseTimeSince(Integer partnerId, LocalDateTime since);

    @Query("SELECT l.endpoint, COUNT(l) FROM PartnerApiLog l WHERE l.partnerId = :partnerId " +
           "AND l.requestTimestamp >= :since GROUP BY l.endpoint ORDER BY COUNT(l) DESC")
    List<Object[]> topEndpointsSince(Integer partnerId, LocalDateTime since);
}
