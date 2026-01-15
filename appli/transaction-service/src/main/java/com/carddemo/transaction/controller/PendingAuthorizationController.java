package com.carddemo.transaction.controller;

import com.carddemo.transaction.dto.AuthorizationStatsResponse;
import com.carddemo.transaction.dto.DeclineAuthorizationRequest;
import com.carddemo.transaction.dto.PendingAuthorizationResponse;
import com.carddemo.transaction.service.PendingAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pending Authorization Controller
 *
 * REST API for pending authorization operations.
 *
 * CICS Transaction to REST Mapping:
 *   COAUTH0C (Auth View)    -> GET /api/v1/authorizations/pending
 *   COAUTH0C (Approve)      -> POST /api/v1/authorizations/{authId}/approve
 *   COAUTH0C (Decline)      -> POST /api/v1/authorizations/{authId}/decline
 */
@RestController
@RequestMapping("/api/v1/authorizations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pending Authorizations", description = "Pending authorization view and actions (COAUTH0C)")
public class PendingAuthorizationController {

    private final PendingAuthorizationService authorizationService;

    @GetMapping("/pending")
    @Operation(
            summary = "List all pending authorizations",
            description = "Returns all pending authorizations. Admin function."
    )
    public ResponseEntity<List<PendingAuthorizationResponse>> getAllPending() {
        log.info("GET /api/v1/authorizations/pending");
        List<PendingAuthorizationResponse> response = authorizationService.getAllPending();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending/account/{accountId}")
    @Operation(
            summary = "List pending authorizations for account",
            description = "Returns pending authorizations for the specified account."
    )
    public ResponseEntity<List<PendingAuthorizationResponse>> getPendingByAccount(
            @Parameter(description = "Account ID")
            @PathVariable Long accountId) {

        log.info("GET /api/v1/authorizations/pending/account/{}", accountId);
        List<PendingAuthorizationResponse> response = authorizationService.getPendingByAccount(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending/customer/{customerId}")
    @Operation(
            summary = "List pending authorizations for customer",
            description = "Returns pending authorizations for the specified customer across all accounts."
    )
    public ResponseEntity<List<PendingAuthorizationResponse>> getPendingByCustomer(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/authorizations/pending/customer/{}", customerId);
        List<PendingAuthorizationResponse> response = authorizationService.getPendingByCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fraud-alerts")
    @Operation(
            summary = "List fraud alerts",
            description = "Returns pending authorizations flagged as potential fraud."
    )
    public ResponseEntity<List<PendingAuthorizationResponse>> getFraudAlerts() {
        log.info("GET /api/v1/authorizations/fraud-alerts");
        List<PendingAuthorizationResponse> response = authorizationService.getFraudAlerts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get authorization statistics",
            description = "Returns aggregate statistics for pending authorizations."
    )
    public ResponseEntity<AuthorizationStatsResponse> getStats() {
        log.info("GET /api/v1/authorizations/stats");
        AuthorizationStatsResponse response = authorizationService.getStats();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/customer/{customerId}")
    @Operation(
            summary = "Get authorization statistics for customer",
            description = "Returns aggregate statistics for a customer's pending authorizations."
    )
    public ResponseEntity<AuthorizationStatsResponse> getStatsByCustomer(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/authorizations/stats/customer/{}", customerId);
        AuthorizationStatsResponse response = authorizationService.getStatsByCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{authId}/approve")
    @Operation(
            summary = "Approve authorization",
            description = "Approves a pending authorization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorization approved"),
            @ApiResponse(responseCode = "400", description = "Cannot approve authorization"),
            @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    public ResponseEntity<PendingAuthorizationResponse> approveAuthorization(
            @Parameter(description = "Authorization ID")
            @PathVariable Long authId) {

        log.info("POST /api/v1/authorizations/{}/approve", authId);
        PendingAuthorizationResponse response = authorizationService.approveAuthorization(authId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{authId}/decline")
    @Operation(
            summary = "Decline authorization",
            description = "Declines a pending authorization with a reason."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authorization declined"),
            @ApiResponse(responseCode = "400", description = "Cannot decline authorization"),
            @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    public ResponseEntity<PendingAuthorizationResponse> declineAuthorization(
            @Parameter(description = "Authorization ID")
            @PathVariable Long authId,
            @Valid @RequestBody DeclineAuthorizationRequest request) {

        log.info("POST /api/v1/authorizations/{}/decline - reason: {}", authId, request.getReason());
        PendingAuthorizationResponse response = authorizationService.declineAuthorization(authId, request.getReason());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{authId}/report-fraud")
    @Operation(
            summary = "Report fraud",
            description = "Reports an authorization as fraudulent. This will also block the card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fraud reported, card blocked"),
            @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    public ResponseEntity<PendingAuthorizationResponse> reportFraud(
            @Parameter(description = "Authorization ID")
            @PathVariable Long authId) {

        log.info("POST /api/v1/authorizations/{}/report-fraud", authId);
        PendingAuthorizationResponse response = authorizationService.reportFraud(authId);
        return ResponseEntity.ok(response);
    }
}
