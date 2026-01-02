package com.carddemo.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook Delivery Entity
 *
 * Maps to the webhook_deliveries table in PostgreSQL.
 * Tracks webhook delivery attempts with exponential backoff retry logic.
 *
 * Status values:
 *   PENDING     - Initial state, awaiting delivery
 *   SUCCESS     - Successfully delivered (HTTP 2xx)
 *   FAILED      - Delivery failed, will retry
 *   DEAD_LETTER - Max retries exceeded
 */
@Entity
@Table(name = "webhook_deliveries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDelivery {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DEAD_LETTER = "DEAD_LETTER";

    public static final int MAX_ATTEMPTS = 5;
    public static final int[] BACKOFF_MINUTES = {1, 5, 15, 60, 240};

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Column(name = "webhook_url", nullable = false, length = 255)
    private String webhookUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        nextAttemptAt = LocalDateTime.now();
    }

    /**
     * Mark delivery as successful
     */
    public void markSuccess() {
        this.status = STATUS_SUCCESS;
        this.completedAt = LocalDateTime.now();
        this.lastError = null;
    }

    /**
     * Mark delivery as failed and schedule retry
     */
    public void markFailed(String error) {
        this.attemptCount++;
        this.lastError = error != null && error.length() > 500 ? error.substring(0, 500) : error;

        if (this.attemptCount >= MAX_ATTEMPTS) {
            this.status = STATUS_DEAD_LETTER;
            this.completedAt = LocalDateTime.now();
        } else {
            this.status = STATUS_FAILED;
            int backoffIndex = Math.min(this.attemptCount - 1, BACKOFF_MINUTES.length - 1);
            this.nextAttemptAt = LocalDateTime.now().plusMinutes(BACKOFF_MINUTES[backoffIndex]);
        }
    }

    /**
     * Check if this delivery can be retried
     */
    public boolean canRetry() {
        return (STATUS_PENDING.equals(status) || STATUS_FAILED.equals(status))
                && attemptCount < MAX_ATTEMPTS;
    }

    /**
     * Check if ready for retry (time-based)
     */
    public boolean isReadyForRetry() {
        return canRetry() && (nextAttemptAt == null || LocalDateTime.now().isAfter(nextAttemptAt));
    }
}
