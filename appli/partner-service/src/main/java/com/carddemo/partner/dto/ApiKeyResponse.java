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
public class ApiKeyResponse {

    private Integer keyId;
    private String maskedKey;       // pk_live_****abcd1234
    private String description;
    private List<String> scopes;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Only populated on creation - the full key is only shown once
    private String apiKey;
}
