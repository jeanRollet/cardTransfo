package com.carddemo.partner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Partner entity - represents external partners (fintechs, merchants, processors)
 * that can access CardDemo APIs via API Keys.
 */
@Entity
@Table(name = "partners")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partner_id")
    private Integer partnerId;

    @Column(name = "partner_name", nullable = false, length = 100)
    private String partnerName;

    @Column(name = "partner_type", nullable = false, length = 20)
    private String partnerType; // FINTECH, MERCHANT, PROCESSOR, BANK

    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    @Column(name = "allowed_scopes", columnDefinition = "text[]")
    private String[] allowedScopes;

    @Column(name = "rate_limit_per_minute")
    @Builder.Default
    private Integer rateLimitPerMinute = 60;

    @Column(name = "daily_quota")
    @Builder.Default
    private Integer dailyQuota = 10000;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PartnerApiKey> apiKeys = new ArrayList<>();

    // Helper methods
    public boolean hasScope(String scope) {
        if (allowedScopes == null) return false;
        for (String s : allowedScopes) {
            if (s.equals(scope) || s.equals("*")) return true;
        }
        return false;
    }

    public String getPartnerTypeName() {
        return switch (partnerType) {
            case "FINTECH" -> "Fintech Partner";
            case "MERCHANT" -> "Merchant";
            case "PROCESSOR" -> "Payment Processor";
            case "BANK" -> "Banking Partner";
            default -> partnerType;
        };
    }
}
