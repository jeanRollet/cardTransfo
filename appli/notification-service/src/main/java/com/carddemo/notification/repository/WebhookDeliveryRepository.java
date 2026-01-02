package com.carddemo.notification.repository;

import com.carddemo.notification.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Webhook Delivery Repository
 */
@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    /**
     * Find deliveries that are ready for retry
     */
    @Query("SELECT wd FROM WebhookDelivery wd " +
            "WHERE wd.status IN ('PENDING', 'FAILED') " +
            "AND wd.attemptCount < :maxAttempts " +
            "AND (wd.nextAttemptAt IS NULL OR wd.nextAttemptAt <= :now) " +
            "ORDER BY wd.nextAttemptAt ASC")
    List<WebhookDelivery> findReadyForRetry(
            @Param("maxAttempts") int maxAttempts,
            @Param("now") LocalDateTime now);

    /**
     * Find deliveries by event ID
     */
    List<WebhookDelivery> findByEventId(UUID eventId);

    /**
     * Find deliveries by partner
     */
    List<WebhookDelivery> findByPartnerPartnerIdOrderByCreatedAtDesc(Integer partnerId);

    /**
     * Find failed deliveries (dead letter)
     */
    List<WebhookDelivery> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Count pending deliveries
     */
    long countByStatus(String status);

    /**
     * Find recent deliveries
     */
    @Query("SELECT wd FROM WebhookDelivery wd " +
            "WHERE wd.createdAt >= :since " +
            "ORDER BY wd.createdAt DESC")
    List<WebhookDelivery> findRecentDeliveries(@Param("since") LocalDateTime since);
}
