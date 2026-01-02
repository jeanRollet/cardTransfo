package com.carddemo.notification.repository;

import com.carddemo.notification.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Webhook Subscription Repository
 */
@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Integer> {

    /**
     * Find active subscriptions for a specific event type
     */
    @Query("SELECT ws FROM WebhookSubscription ws " +
            "JOIN FETCH ws.partner p " +
            "WHERE ws.eventType = :eventType " +
            "AND ws.isActive = true " +
            "AND p.isActive = true " +
            "AND p.webhookUrl IS NOT NULL")
    List<WebhookSubscription> findActiveSubscriptionsByEventType(@Param("eventType") String eventType);

    /**
     * Find all subscriptions for a partner
     */
    List<WebhookSubscription> findByPartnerPartnerIdAndIsActiveTrue(Integer partnerId);
}
