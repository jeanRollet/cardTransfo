package com.carddemo.partner.controller;

import com.carddemo.partner.dto.*;
import com.carddemo.partner.service.ApiKeyService;
import com.carddemo.partner.service.PartnerService;
import com.carddemo.partner.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Partner Management API - Admin endpoints for managing partners and API keys.
 * In production, these would be protected by JWT authentication for admin users.
 */
@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Tag(name = "Partner Management", description = "Admin endpoints for managing partners and API keys")
public class PartnerController {

    private final PartnerService partnerService;
    private final ApiKeyService apiKeyService;
    private final UsageService usageService;

    // ========== Partner CRUD ==========

    @PostMapping
    @Operation(summary = "Register a new partner", description = "Create a new partner account")
    public ResponseEntity<PartnerResponse> createPartner(@Valid @RequestBody PartnerRequest request) {
        PartnerResponse partner = partnerService.createPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(partner);
    }

    @GetMapping
    @Operation(summary = "List all partners", description = "Get all active partners")
    public ResponseEntity<List<PartnerResponse>> getAllPartners() {
        return ResponseEntity.ok(partnerService.getAllPartners());
    }

    @GetMapping("/{partnerId}")
    @Operation(summary = "Get partner details", description = "Get details of a specific partner")
    public ResponseEntity<PartnerResponse> getPartner(@PathVariable Integer partnerId) {
        return ResponseEntity.ok(partnerService.getPartner(partnerId));
    }

    @PutMapping("/{partnerId}")
    @Operation(summary = "Update partner", description = "Update partner information")
    public ResponseEntity<PartnerResponse> updatePartner(
            @PathVariable Integer partnerId,
            @Valid @RequestBody PartnerRequest request) {
        return ResponseEntity.ok(partnerService.updatePartner(partnerId, request));
    }

    @DeleteMapping("/{partnerId}")
    @Operation(summary = "Deactivate partner", description = "Deactivate a partner (soft delete)")
    public ResponseEntity<Void> deactivatePartner(@PathVariable Integer partnerId) {
        partnerService.deactivatePartner(partnerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search partners", description = "Search partners by name")
    public ResponseEntity<List<PartnerResponse>> searchPartners(@RequestParam String name) {
        return ResponseEntity.ok(partnerService.searchPartners(name));
    }

    // ========== API Key Management ==========

    @PostMapping("/{partnerId}/keys")
    @Operation(summary = "Generate API key", description = "Generate a new API key for a partner. The full key is only shown once!")
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @PathVariable Integer partnerId,
            @Valid @RequestBody ApiKeyRequest request) {
        ApiKeyResponse response = apiKeyService.createApiKey(partnerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{partnerId}/keys")
    @Operation(summary = "List API keys", description = "Get all API keys for a partner (keys are masked)")
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys(@PathVariable Integer partnerId) {
        return ResponseEntity.ok(apiKeyService.getPartnerApiKeys(partnerId));
    }

    @DeleteMapping("/{partnerId}/keys/{keyId}")
    @Operation(summary = "Revoke API key", description = "Revoke an API key")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable Integer partnerId,
            @PathVariable Integer keyId) {
        apiKeyService.revokeApiKey(partnerId, keyId);
        return ResponseEntity.noContent().build();
    }

    // ========== Usage & Stats ==========

    @GetMapping("/{partnerId}/usage")
    @Operation(summary = "Get usage statistics", description = "Get API usage statistics for a partner")
    public ResponseEntity<UsageResponse> getUsage(
            @PathVariable Integer partnerId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(usageService.getUsage(partnerId, days));
    }
}
