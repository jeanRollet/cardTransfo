package com.carddemo.partner.controller;

import com.carddemo.partner.dto.ApiKeyValidationResponse;
import com.carddemo.partner.service.ApiKeyService;
import com.carddemo.partner.service.RateLimitService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API endpoints for use by other microservices.
 * These endpoints are not exposed to external partners.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Hidden // Hide from Swagger documentation
public class InternalController {

    private final ApiKeyService apiKeyService;
    private final RateLimitService rateLimitService;

    /**
     * Validate an API key and return partner information.
     * Used by the API Gateway filter in other services.
     */
    @GetMapping("/validate-key")
    public ResponseEntity<ApiKeyValidationResponse> validateApiKey(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        return ResponseEntity.ok(apiKeyService.validateApiKey(apiKey));
    }

    /**
     * Check rate limit for a partner.
     * Returns limit status and headers.
     */
    @GetMapping("/check-rate-limit/{partnerId}")
    public ResponseEntity<RateLimitResponse> checkRateLimit(
            @PathVariable Integer partnerId,
            @RequestParam int limitPerMinute,
            @RequestParam int dailyQuota) {

        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(
                partnerId, limitPerMinute, dailyQuota);

        RateLimitResponse response = new RateLimitResponse(
                result.limited(),
                result.errorCode(),
                result.message(),
                result.retryAfterSeconds(),
                result.limit(),
                result.remaining()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Record API usage for a partner (called after request completes)
     */
    @PostMapping("/record-usage/{partnerId}")
    public ResponseEntity<Void> recordUsage(
            @PathVariable Integer partnerId,
            @RequestParam boolean success) {
        rateLimitService.recordUsage(partnerId, success);
        return ResponseEntity.ok().build();
    }

    public record RateLimitResponse(
            boolean limited,
            String errorCode,
            String message,
            long retryAfterSeconds,
            int limit,
            long remaining
    ) {}
}
