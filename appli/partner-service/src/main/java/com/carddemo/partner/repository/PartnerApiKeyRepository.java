package com.carddemo.partner.repository;

import com.carddemo.partner.entity.PartnerApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerApiKeyRepository extends JpaRepository<PartnerApiKey, Integer> {

    Optional<PartnerApiKey> findByApiKeyHashAndIsActiveTrue(String apiKeyHash);

    List<PartnerApiKey> findByPartnerPartnerIdAndIsActiveTrue(Integer partnerId);

    List<PartnerApiKey> findByPartnerPartnerId(Integer partnerId);

    @Modifying
    @Query("UPDATE PartnerApiKey k SET k.lastUsedAt = :timestamp WHERE k.keyId = :keyId")
    void updateLastUsedAt(Integer keyId, LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE PartnerApiKey k SET k.isActive = false WHERE k.keyId = :keyId")
    void deactivateKey(Integer keyId);

    @Query("SELECT COUNT(k) FROM PartnerApiKey k WHERE k.partner.partnerId = :partnerId AND k.isActive = true")
    long countActiveKeysByPartnerId(Integer partnerId);
}
