package com.carddemo.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerResponse {

    private Integer partnerId;
    private String partnerName;
    private String partnerType;
    private String partnerTypeName;
    private String contactEmail;
    private String contactPhone;
    private String webhookUrl;
    private List<String> allowedScopes;
    private Integer rateLimitPerMinute;
    private Integer dailyQuota;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer activeKeyCount;
}
