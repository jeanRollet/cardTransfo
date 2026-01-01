package com.carddemo.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyValidationResponse {

    private boolean valid;
    private Integer partnerId;
    private String partnerName;
    private String partnerType;
    private List<String> scopes;
    private Integer rateLimitPerMinute;
    private Integer dailyQuota;
    private String errorCode;
    private String errorMessage;

    public static ApiKeyValidationResponse invalid(String errorCode, String errorMessage) {
        return ApiKeyValidationResponse.builder()
                .valid(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}
