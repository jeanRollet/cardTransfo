package com.carddemo.partner.controller;

import com.carddemo.partner.dto.ApiKeyValidationResponse;
import com.carddemo.partner.exception.PartnerException;
import com.carddemo.partner.service.ApiKeyService;
import com.carddemo.partner.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Partner API Gateway - Proxies requests to backend services with API key authentication.
 *
 * This controller handles all /partner/v1/* requests:
 * 1. Validates the API key
 * 2. Checks rate limits
 * 3. Verifies scopes
 * 4. Proxies the request to the appropriate backend service
 * 5. Returns the response with rate limit headers
 */
@RestController
@RequestMapping("/partner/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Partner API", description = "Partner-facing API endpoints with API key authentication")
public class PartnerApiController {

    private final ApiKeyService apiKeyService;
    private final RateLimitService rateLimitService;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.account-service}")
    private String accountServiceUrl;

    @Value("${services.card-service}")
    private String cardServiceUrl;

    @Value("${services.transaction-service}")
    private String transactionServiceUrl;

    // ========== Account Endpoints ==========

    @GetMapping("/accounts/{accountId}")
    @Operation(summary = "Get account details", description = "Retrieve account information (requires accounts:read scope)")
    public Mono<ResponseEntity<Object>> getAccount(
            @Parameter(hidden = true) @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long accountId,
            HttpServletRequest request) {

        return executeWithAuth(apiKey, "accounts:read", request, () ->
                proxyGet(accountServiceUrl + "/api/v1/accounts/" + accountId));
    }

    @GetMapping("/accounts/{accountId}/balance")
    @Operation(summary = "Get account balance", description = "Retrieve simplified balance info (requires accounts:read scope)")
    public Mono<ResponseEntity<Object>> getAccountBalance(
            @Parameter(hidden = true) @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long accountId,
            HttpServletRequest request) {

        return executeWithAuth(apiKey, "accounts:read", request, () ->
                proxyGet(accountServiceUrl + "/api/v1/accounts/" + accountId)
                        .map(response -> {
                            // Transform to simplified balance response
                            // In real implementation, would parse and filter fields
                            return response;
                        }));
    }

    // ========== Transaction Endpoints ==========

    @GetMapping("/transactions/account/{accountId}")
    @Operation(summary = "Get account transactions", description = "Retrieve transaction history (requires transactions:read scope)")
    public Mono<ResponseEntity<Object>> getAccountTransactions(
            @Parameter(hidden = true) @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        String url = String.format("%s/api/v1/transactions/account/%d?page=%d&size=%d",
                transactionServiceUrl, accountId, page, size);

        return executeWithAuth(apiKey, "transactions:read", request, () -> proxyGet(url));
    }

    @GetMapping("/transactions/{transactionId}")
    @Operation(summary = "Get transaction details", description = "Retrieve a specific transaction (requires transactions:read scope)")
    public Mono<ResponseEntity<Object>> getTransaction(
            @Parameter(hidden = true) @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long transactionId,
            HttpServletRequest request) {

        return executeWithAuth(apiKey, "transactions:read", request, () ->
                proxyGet(transactionServiceUrl + "/api/v1/transactions/" + transactionId));
    }

    // ========== Card Endpoints ==========

    @GetMapping("/cards/account/{accountId}")
    @Operation(summary = "Get account cards", description = "Retrieve cards for an account (requires cards:read scope)")
    public Mono<ResponseEntity<Object>> getAccountCards(
            @Parameter(hidden = true) @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long accountId,
            HttpServletRequest request) {

        return executeWithAuth(apiKey, "cards:read", request, () ->
                proxyGet(cardServiceUrl + "/api/v1/cards/account/" + accountId));
    }

    // ========== Helper Methods ==========

    private Mono<ResponseEntity<Object>> executeWithAuth(
            String apiKey,
            String requiredScope,
            HttpServletRequest request,
            java.util.function.Supplier<Mono<ResponseEntity<Object>>> action) {

        // 1. Validate API key
        ApiKeyValidationResponse validation = apiKeyService.validateApiKey(apiKey);
        if (!validation.isValid()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse(validation.getErrorCode(), validation.getErrorMessage())));
        }

        // 2. Check scope
        if (!validation.getScopes().contains(requiredScope) && !validation.getScopes().contains("*")) {
            throw PartnerException.insufficientScope(requiredScope);
        }

        // 3. Check rate limit
        RateLimitService.RateLimitResult rateLimit = rateLimitService.checkRateLimit(
                validation.getPartnerId(),
                validation.getRateLimitPerMinute(),
                validation.getDailyQuota()
        );

        if (rateLimit.limited()) {
            HttpHeaders headers = createRateLimitHeaders(rateLimit);
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(h -> h.addAll(headers))
                    .body(createErrorResponse(rateLimit.errorCode(), rateLimit.message())));
        }

        // 4. Execute the action and add rate limit headers
        return action.get()
                .map(response -> {
                    HttpHeaders headers = createRateLimitHeaders(rateLimit);
                    return ResponseEntity.status(response.getStatusCode())
                            .headers(h -> h.addAll(headers))
                            .body(response.getBody());
                })
                .doOnSuccess(r -> rateLimitService.recordUsage(validation.getPartnerId(), r.getStatusCode().is2xxSuccessful()))
                .doOnError(e -> rateLimitService.recordUsage(validation.getPartnerId(), false));
    }

    private Mono<ResponseEntity<Object>> proxyGet(String url) {
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .toEntity(Object.class)
                .onErrorResume(e -> {
                    log.error("Error proxying request to {}: {}", url, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .body(createErrorResponse("UPSTREAM_ERROR", "Error communicating with backend service")));
                });
    }

    private HttpHeaders createRateLimitHeaders(RateLimitService.RateLimitResult rateLimit) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(rateLimit.limit()));
        headers.add("X-RateLimit-Remaining", String.valueOf(rateLimit.remaining()));
        if (rateLimit.retryAfterSeconds() > 0) {
            headers.add("Retry-After", String.valueOf(rateLimit.retryAfterSeconds()));
        }
        return headers;
    }

    private Object createErrorResponse(String errorCode, String message) {
        return new ErrorResponse(errorCode, message);
    }

    private record ErrorResponse(String errorCode, String message) {}
}
