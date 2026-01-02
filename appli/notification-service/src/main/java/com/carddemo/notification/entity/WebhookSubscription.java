package com.carddemo.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Webhook Subscription Entity
 *
 * Maps to the webhook_subscriptions table in PostgreSQL.
 * Defines which event types a partner wants to receive.
 */
@Entity
@Table(name = "webhook_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Integer subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "scope_required", nullable = false, length = 50)
    private String scopeRequired;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
