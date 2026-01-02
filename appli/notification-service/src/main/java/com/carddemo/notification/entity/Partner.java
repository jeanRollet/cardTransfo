package com.carddemo.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Partner Entity (read-only view)
 *
 * Maps to the partners table in PostgreSQL.
 */
@Entity
@Table(name = "partners")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @Column(name = "partner_id")
    private Integer partnerId;

    @Column(name = "partner_name", nullable = false, unique = true, length = 100)
    private String partnerName;

    @Column(name = "partner_type", nullable = false, length = 20)
    private String partnerType;

    @Column(name = "contact_email", nullable = false, unique = true, length = 100)
    private String contactEmail;

    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    @Column(name = "allowed_scopes", columnDefinition = "TEXT[]")
    private String[] allowedScopes;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "partner", fetch = FetchType.LAZY)
    private List<WebhookSubscription> subscriptions;

    /**
     * Check if partner has a specific scope
     */
    public boolean hasScope(String scope) {
        if (allowedScopes == null) return false;
        for (String s : allowedScopes) {
            if (s.equals(scope)) return true;
        }
        return false;
    }
}
