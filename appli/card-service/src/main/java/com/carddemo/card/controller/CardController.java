package com.carddemo.card.controller;

import com.carddemo.card.dto.CardListResponse;
import com.carddemo.card.dto.CardResponse;
import com.carddemo.card.dto.UpdateCardStatusRequest;
import com.carddemo.card.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Card Controller
 *
 * REST API for credit card management operations.
 *
 * CICS Transaction to REST Mapping:
 *   COCRDLIC (Card List)   -> GET /api/v1/cards/account/{accountId}
 *   COCRDSLC (Card Select) -> GET /api/v1/cards/{cardNumber}
 *   COCRDUPC (Card Update) -> PUT /api/v1/cards/{cardNumber}/status
 *   COCRDBLK (Card Block)  -> POST /api/v1/cards/{cardNumber}/block
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Card Management", description = "Credit card operations (COCRDLIC/COCRDSLC/COCRDUPC)")
public class CardController {

    private final CardService cardService;

    /**
     * Get all cards for an account (COCRDLIC - Card List)
     */
    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "List cards for account",
            description = "Returns all credit cards associated with the specified account. Replaces CICS COCRDLIC transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<CardListResponse> getCardsByAccount(
            @Parameter(description = "Account ID (11 digits)")
            @PathVariable Long accountId) {

        log.info("GET /api/v1/cards/account/{}", accountId);
        CardListResponse response = cardService.getCardsByAccount(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all cards for a customer (across all their accounts)
     */
    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "List cards for customer",
            description = "Returns all credit cards for a customer across all their accounts."
    )
    public ResponseEntity<List<CardResponse>> getCardsByCustomer(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/cards/customer/{}", customerId);
        List<CardResponse> response = cardService.getCardsByCustomerId(customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all cards with pagination (Admin view)
     */
    @GetMapping
    @Operation(
            summary = "List all cards",
            description = "Returns all credit cards with pagination. Admin function."
    )
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @PageableDefault(size = 20, sort = "issuedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("GET /api/v1/cards - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<CardResponse> response = cardService.getAllCards(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get cards by status
     */
    @GetMapping("/status/{status}")
    @Operation(
            summary = "List cards by status",
            description = "Returns cards filtered by status: Y=Active, N=Closed, S=Blocked"
    )
    public ResponseEntity<Page<CardResponse>> getCardsByStatus(
            @Parameter(description = "Card status: Y, N, or S")
            @PathVariable String status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/cards/status/{}", status);
        Page<CardResponse> response = cardService.getCardsByStatus(status.toUpperCase(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get card details (COCRDSLC - Card Select)
     */
    @GetMapping("/{cardNumber}")
    @Operation(
            summary = "Get card details",
            description = "Returns detailed information for a specific card. Replaces CICS COCRDSLC transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card details retrieved"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<CardResponse> getCardDetails(
            @Parameter(description = "Card number (16 digits)")
            @PathVariable String cardNumber) {

        log.info("GET /api/v1/cards/{}", maskCardNumber(cardNumber));
        CardResponse response = cardService.getCardDetails(cardNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Update card status (COCRDUPC - Card Update)
     */
    @PutMapping("/{cardNumber}/status")
    @Operation(
            summary = "Update card status",
            description = "Updates card status (activate/deactivate/block). Replaces CICS COCRDUPC transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<CardResponse> updateCardStatus(
            @Parameter(description = "Card number (16 digits)")
            @PathVariable String cardNumber,
            @Valid @RequestBody UpdateCardStatusRequest request) {

        log.info("PUT /api/v1/cards/{}/status -> {}", maskCardNumber(cardNumber), request.getStatus());
        CardResponse response = cardService.updateCardStatus(cardNumber, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Block card (Emergency block)
     */
    @PostMapping("/{cardNumber}/block")
    @Operation(
            summary = "Block card",
            description = "Emergency block for lost/stolen cards. Immediately blocks the card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Card blocked successfully"),
            @ApiResponse(responseCode = "400", description = "Card already blocked"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<CardResponse> blockCard(
            @Parameter(description = "Card number (16 digits)")
            @PathVariable String cardNumber,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.get("reason") : "Emergency block";
        log.warn("POST /api/v1/cards/{}/block - reason: {}", maskCardNumber(cardNumber), reason);
        CardResponse response = cardService.blockCard(cardNumber, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Get cards expiring soon
     */
    @GetMapping("/expiring")
    @Operation(
            summary = "List expiring cards",
            description = "Returns cards expiring within the specified number of days"
    )
    public ResponseEntity<List<CardResponse>> getExpiringCards(
            @Parameter(description = "Days ahead to check (default: 30)")
            @RequestParam(defaultValue = "30") int days) {

        log.info("GET /api/v1/cards/expiring?days={}", days);
        List<CardResponse> response = cardService.getCardsExpiringSoon(days);
        return ResponseEntity.ok(response);
    }

    /**
     * Search cards by cardholder name
     */
    @GetMapping("/search")
    @Operation(
            summary = "Search cards by name",
            description = "Searches cards by embossed name (partial match)"
    )
    public ResponseEntity<List<CardResponse>> searchCards(
            @Parameter(description = "Name to search for")
            @RequestParam String name) {

        log.info("GET /api/v1/cards/search?name={}", name);
        List<CardResponse> response = cardService.searchByName(name);
        return ResponseEntity.ok(response);
    }

    /**
     * Get card by last 4 digits (for frontend navigation)
     */
    @GetMapping("/by-last-four/{lastFour}")
    @Operation(
            summary = "Get card by last 4 digits",
            description = "Returns card details by last 4 digits. Used for frontend navigation."
    )
    public ResponseEntity<CardResponse> getCardByLastFour(
            @Parameter(description = "Last 4 digits of card number")
            @PathVariable String lastFour) {

        log.info("GET /api/v1/cards/by-last-four/{}", lastFour);
        CardResponse response = cardService.getCardByLastFour(lastFour);
        return ResponseEntity.ok(response);
    }

    /**
     * Update card status by last 4 digits
     */
    @PutMapping("/by-last-four/{lastFour}/status")
    @Operation(
            summary = "Update card status by last 4 digits",
            description = "Updates card status using last 4 digits for identification"
    )
    public ResponseEntity<CardResponse> updateCardStatusByLastFour(
            @Parameter(description = "Last 4 digits of card number")
            @PathVariable String lastFour,
            @Valid @RequestBody UpdateCardStatusRequest request) {

        log.info("PUT /api/v1/cards/by-last-four/{}/status -> {}", lastFour, request.getStatus());
        CardResponse response = cardService.updateCardStatusByLastFour(lastFour, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Block card by last 4 digits
     */
    @PostMapping("/by-last-four/{lastFour}/block")
    @Operation(
            summary = "Block card by last 4 digits",
            description = "Emergency block using last 4 digits for identification"
    )
    public ResponseEntity<CardResponse> blockCardByLastFour(
            @Parameter(description = "Last 4 digits of card number")
            @PathVariable String lastFour,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.get("reason") : "Emergency block";
        log.warn("POST /api/v1/cards/by-last-four/{}/block - reason: {}", lastFour, reason);
        CardResponse response = cardService.blockCardByLastFour(lastFour, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Reissue a card by last 4 digits
     */
    @PostMapping("/by-last-four/{lastFour}/reissue")
    @Operation(
            summary = "Request card reissue",
            description = "Creates a request to reissue the card with a new number"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reissue request submitted"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<CardResponse> reissueCardByLastFour(
            @Parameter(description = "Last 4 digits of card number")
            @PathVariable String lastFour) {

        log.info("POST /api/v1/cards/by-last-four/{}/reissue", lastFour);
        CardResponse response = cardService.reissueCardByLastFour(lastFour);
        return ResponseEntity.ok(response);
    }

    // Helper method to mask card number in logs
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}
