package com.carddemo.partner.service;

import com.carddemo.partner.dto.PartnerRequest;
import com.carddemo.partner.dto.PartnerResponse;
import com.carddemo.partner.entity.Partner;
import com.carddemo.partner.exception.PartnerException;
import com.carddemo.partner.repository.PartnerApiKeyRepository;
import com.carddemo.partner.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerApiKeyRepository apiKeyRepository;

    @Transactional
    public PartnerResponse createPartner(PartnerRequest request) {
        // Validate uniqueness
        if (partnerRepository.existsByPartnerName(request.getPartnerName())) {
            throw PartnerException.partnerNameExists(request.getPartnerName());
        }
        if (partnerRepository.existsByContactEmail(request.getContactEmail())) {
            throw PartnerException.emailExists(request.getContactEmail());
        }

        Partner partner = Partner.builder()
                .partnerName(request.getPartnerName())
                .partnerType(request.getPartnerType())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .webhookUrl(request.getWebhookUrl())
                .allowedScopes(request.getAllowedScopes() != null ?
                        request.getAllowedScopes().toArray(new String[0]) : new String[]{"accounts:read", "transactions:read"})
                .rateLimitPerMinute(request.getRateLimitPerMinute() != null ? request.getRateLimitPerMinute() : 60)
                .dailyQuota(request.getDailyQuota() != null ? request.getDailyQuota() : 10000)
                .isActive(true)
                .build();

        partner = partnerRepository.save(partner);
        log.info("Created new partner: {} (ID: {})", partner.getPartnerName(), partner.getPartnerId());

        return toResponse(partner);
    }

    @Transactional(readOnly = true)
    public List<PartnerResponse> getAllPartners() {
        return partnerRepository.findByIsActiveTrueOrderByPartnerNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PartnerResponse getPartner(Integer partnerId) {
        Partner partner = partnerRepository.findByPartnerIdAndIsActiveTrue(partnerId)
                .orElseThrow(() -> PartnerException.partnerNotFound(partnerId));
        return toResponse(partner);
    }

    @Transactional
    public PartnerResponse updatePartner(Integer partnerId, PartnerRequest request) {
        Partner partner = partnerRepository.findByPartnerIdAndIsActiveTrue(partnerId)
                .orElseThrow(() -> PartnerException.partnerNotFound(partnerId));

        // Update fields
        if (request.getPartnerName() != null && !request.getPartnerName().equals(partner.getPartnerName())) {
            if (partnerRepository.existsByPartnerName(request.getPartnerName())) {
                throw PartnerException.partnerNameExists(request.getPartnerName());
            }
            partner.setPartnerName(request.getPartnerName());
        }
        if (request.getPartnerType() != null) {
            partner.setPartnerType(request.getPartnerType());
        }
        if (request.getContactEmail() != null && !request.getContactEmail().equals(partner.getContactEmail())) {
            if (partnerRepository.existsByContactEmail(request.getContactEmail())) {
                throw PartnerException.emailExists(request.getContactEmail());
            }
            partner.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            partner.setContactPhone(request.getContactPhone());
        }
        if (request.getWebhookUrl() != null) {
            partner.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getAllowedScopes() != null) {
            partner.setAllowedScopes(request.getAllowedScopes().toArray(new String[0]));
        }
        if (request.getRateLimitPerMinute() != null) {
            partner.setRateLimitPerMinute(request.getRateLimitPerMinute());
        }
        if (request.getDailyQuota() != null) {
            partner.setDailyQuota(request.getDailyQuota());
        }

        partner = partnerRepository.save(partner);
        log.info("Updated partner: {} (ID: {})", partner.getPartnerName(), partner.getPartnerId());

        return toResponse(partner);
    }

    @Transactional
    public void deactivatePartner(Integer partnerId) {
        Partner partner = partnerRepository.findByPartnerIdAndIsActiveTrue(partnerId)
                .orElseThrow(() -> PartnerException.partnerNotFound(partnerId));

        partner.setIsActive(false);
        partnerRepository.save(partner);
        log.info("Deactivated partner: {} (ID: {})", partner.getPartnerName(), partner.getPartnerId());
    }

    @Transactional(readOnly = true)
    public List<PartnerResponse> searchPartners(String name) {
        return partnerRepository.searchByName(name)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PartnerResponse toResponse(Partner partner) {
        long activeKeyCount = apiKeyRepository.countActiveKeysByPartnerId(partner.getPartnerId());

        return PartnerResponse.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerType(partner.getPartnerType())
                .partnerTypeName(partner.getPartnerTypeName())
                .contactEmail(partner.getContactEmail())
                .contactPhone(partner.getContactPhone())
                .webhookUrl(partner.getWebhookUrl())
                .allowedScopes(partner.getAllowedScopes() != null ?
                        Arrays.asList(partner.getAllowedScopes()) : List.of())
                .rateLimitPerMinute(partner.getRateLimitPerMinute())
                .dailyQuota(partner.getDailyQuota())
                .isActive(partner.getIsActive())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .activeKeyCount((int) activeKeyCount)
                .build();
    }
}
