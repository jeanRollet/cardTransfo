package com.carddemo.partner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Partner API Key entity - stores hashed API keys for partner authentication.
 * Keys are never stored in plain text, only the SHA-256 hash.
 */
@Entity
@Table(name = "partner_api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "key_id")
    private Integer keyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    @ToString.Exclude
    private Partner partner;

    @Column(name = "api_key_hash", nullable = false, length = 256)
    private String apiKeyHash; // SHA-256 hash of the API key

    @Column(name = "key_prefix", nullable = false, length = 12)
    private String keyPrefix; // e.g., "pk_live_" or "pk_test_"

    @Column(name = "key_suffix", length = 8)
    private String keySuffix; // Last 8 chars for identification

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "scopes", columnDefinition = "text[]")
    private String[] scopes; // Specific scopes for this key (subset of partner's allowed scopes)

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    public boolean hasScope(String scope) {
        if (scopes == null || scopes.length == 0) {
            // If no specific scopes, inherit from partner
            return partner.hasScope(scope);
        }
        for (String s : scopes) {
            if (s.equals(scope) || s.equals("*")) return true;
        }
        return false;
    }

    public String getMaskedKey() {
        return keyPrefix + "****" + (keySuffix != null ? keySuffix : "");
    }
}
