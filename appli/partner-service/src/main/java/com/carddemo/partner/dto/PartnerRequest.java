package com.carddemo.partner.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerRequest {

    @NotBlank(message = "Partner name is required")
    @Size(max = 100, message = "Partner name must be at most 100 characters")
    private String partnerName;

    @NotBlank(message = "Partner type is required")
    @Pattern(regexp = "^(FINTECH|MERCHANT|PROCESSOR|BANK)$",
             message = "Partner type must be FINTECH, MERCHANT, PROCESSOR, or BANK")
    private String partnerType;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String contactEmail;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String contactPhone;

    @Size(max = 255, message = "Webhook URL must be at most 255 characters")
    private String webhookUrl;

    private List<String> allowedScopes;

    @Min(value = 1, message = "Rate limit must be at least 1")
    @Max(value = 1000, message = "Rate limit cannot exceed 1000 per minute")
    private Integer rateLimitPerMinute;

    @Min(value = 100, message = "Daily quota must be at least 100")
    @Max(value = 1000000, message = "Daily quota cannot exceed 1,000,000")
    private Integer dailyQuota;
}
