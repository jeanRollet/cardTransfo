package com.carddemo.partner.service;

import com.carddemo.partner.dto.ApiKeyRequest;
import com.carddemo.partner.dto.ApiKeyResponse;
import com.carddemo.partner.dto.ApiKeyValidationResponse;
import com.carddemo.partner.entity.Partner;
import com.carddemo.partner.entity.PartnerApiKey;
import com.carddemo.partner.exception.PartnerException;
import com.carddemo.partner.repository.PartnerApiKeyRepository;
import com.carddemo.partner.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final PartnerRepository partnerRepository;
    private final PartnerApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${partner.api.key-prefix:pk_live_}")
    private String keyPrefix;

    @Value("${partner.api.key-length:32}")
    private int keyLength;

    @Transactional
    public ApiKeyResponse createApiKey(Integer partnerId, ApiKeyRequest request) {
        Partner partner = partnerRepository.findByPartnerIdAndIsActiveTrue(partnerId)
                .orElseThrow(() -> PartnerException.partnerNotFound(partnerId));

        // Validate scopes against partner's allowed scopes
        if (request.getScopes() != null) {
            for (String scope : request.getScopes()) {
                if (!partner.hasScope(scope)) {
                    throw PartnerException.invalidScope(scope);
                }
            }
        }

        // Generate API key
        String rawKey = generateRandomKey();
        String fullKey = keyPrefix + rawKey;
        String keyHash = hashApiKey(fullKey);
        String keySuffix = rawKey.substring(rawKey.length() - 8);

        PartnerApiKey apiKey = PartnerApiKey.builder()
                .partner(partner)
                .apiKeyHash(keyHash)
                .keyPrefix(keyPrefix)
                .keySuffix(keySuffix)
                .description(request.getDescription())
                .scopes(request.getScopes() != null ?
                        request.getScopes().toArray(new String[0]) : null)
                .expiresAt(request.getExpiresInDays() != null ?
                        LocalDateTime.now().plusDays(request.getExpiresInDays()) : null)
                .isActive(true)
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Created API key for partner {} (key ID: {})", partnerId, apiKey.getKeyId());

        return toResponse(apiKey, fullKey); // Include full key only on creation
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getPartnerApiKeys(Integer partnerId) {
        // Verify partner exists
        partnerRepository.findByPartnerIdAndIsActiveTrue(partnerId)
                .orElseThrow(() -> PartnerException.partnerNotFound(partnerId));

        return apiKeyRepository.findByPartnerPartnerId(partnerId)
                .stream()
                .map(key -> toResponse(key, null)) // Never expose the actual key
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeApiKey(Integer partnerId, Integer keyId) {
        PartnerApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> PartnerException.apiKeyNotFound(keyId));

        if (!apiKey.getPartner().getPartnerId().equals(partnerId)) {
            throw PartnerException.apiKeyNotFound(keyId);
        }

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        log.info("Revoked API key {} for partner {}", keyId, partnerId);
    }

    @Transactional
    public ApiKeyValidationResponse validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return ApiKeyValidationResponse.invalid("MISSING_API_KEY", "API key is required");
        }

        String keyHash = hashApiKey(apiKey);
        PartnerApiKey key = apiKeyRepository.findByApiKeyHashAndIsActiveTrue(keyHash)
                .orElse(null);

        if (key == null) {
            return ApiKeyValidationResponse.invalid("INVALID_API_KEY", "API key is invalid or has been revoked");
        }

        if (key.isExpired()) {
            return ApiKeyValidationResponse.invalid("EXPIRED_API_KEY", "API key has expired");
        }

        Partner partner = key.getPartner();
        if (!partner.getIsActive()) {
            return ApiKeyValidationResponse.invalid("PARTNER_INACTIVE", "Partner account is inactive");
        }

        // Update last used timestamp
        apiKeyRepository.updateLastUsedAt(key.getKeyId(), LocalDateTime.now());

        // Determine effective scopes
        String[] effectiveScopes = key.getScopes() != null && key.getScopes().length > 0 ?
                key.getScopes() : partner.getAllowedScopes();

        return ApiKeyValidationResponse.builder()
                .valid(true)
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerType(partner.getPartnerType())
                .scopes(effectiveScopes != null ? Arrays.asList(effectiveScopes) : List.of())
                .rateLimitPerMinute(partner.getRateLimitPerMinute())
                .dailyQuota(partner.getDailyQuota())
                .build();
    }

    private String generateRandomKey() {
        byte[] randomBytes = new byte[keyLength];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
                .substring(0, keyLength);
    }

    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private ApiKeyResponse toResponse(PartnerApiKey key, String fullKey) {
        return ApiKeyResponse.builder()
                .keyId(key.getKeyId())
                .maskedKey(key.getMaskedKey())
                .description(key.getDescription())
                .scopes(key.getScopes() != null ? Arrays.asList(key.getScopes()) : null)
                .expiresAt(key.getExpiresAt())
                .lastUsedAt(key.getLastUsedAt())
                .isActive(key.getIsActive())
                .createdAt(key.getCreatedAt())
                .apiKey(fullKey) // Only set on creation
                .build();
    }
}
