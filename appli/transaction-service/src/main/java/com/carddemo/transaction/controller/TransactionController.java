package com.carddemo.transaction.controller;

import com.carddemo.transaction.dto.CreateTransactionRequest;
import com.carddemo.transaction.dto.TransactionListResponse;
import com.carddemo.transaction.dto.TransactionResponse;
import com.carddemo.transaction.dto.TransactionSummaryResponse;
import com.carddemo.transaction.service.TransactionService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Transaction Controller
 *
 * REST API for transaction history and reporting.
 *
 * CICS Transaction to REST Mapping:
 *   COTRN00C (Transaction History) -> GET /api/v1/transactions/account/{accountId}
 *   CT01 (Transaction View)        -> GET /api/v1/transactions/{transactionId}
 *   CORPT00C (Reports)             -> GET /api/v1/transactions/summary
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "Transaction history and reporting (COTRN00C/CORPT00C)")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Get all transactions with pagination (Admin view)
     */
    @GetMapping
    @Operation(
            summary = "List all transactions",
            description = "Returns all transactions with pagination. Admin function."
    )
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("GET /api/v1/transactions - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<TransactionResponse> response = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transactions for an account (COTRN00C - Transaction History)
     */
    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "List transactions for account",
            description = "Returns transaction history for the specified account. Replaces CICS COTRN00C transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<TransactionListResponse> getTransactionsByAccount(
            @Parameter(description = "Account ID (11 digits)")
            @PathVariable Long accountId) {

        log.info("GET /api/v1/transactions/account/{}", accountId);
        TransactionListResponse response = transactionService.getTransactionsByAccount(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transactions for a customer (across all their accounts) - for RBAC
     */
    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "List transactions for customer",
            description = "Returns all transactions for a customer across all their accounts."
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByCustomer(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/transactions/customer/{}", customerId);
        List<TransactionResponse> response = transactionService.getTransactionsByCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction details
     */
    @GetMapping("/{transactionId}")
    @Operation(
            summary = "Get transaction details",
            description = "Returns detailed information for a specific transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction details retrieved"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<TransactionResponse> getTransactionById(
            @Parameter(description = "Transaction ID")
            @PathVariable Long transactionId) {

        log.info("GET /api/v1/transactions/{}", transactionId);
        TransactionResponse response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transactions by date range
     */
    @GetMapping("/date-range")
    @Operation(
            summary = "List transactions by date range",
            description = "Returns transactions within the specified date range"
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/v1/transactions/date-range?startDate={}&endDate={}", startDate, endDate);
        List<TransactionResponse> response = transactionService.getTransactionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Search transactions
     */
    @GetMapping("/search")
    @Operation(
            summary = "Search transactions",
            description = "Searches transactions by description or merchant name"
    )
    public ResponseEntity<List<TransactionResponse>> searchTransactions(
            @Parameter(description = "Search term")
            @RequestParam String term) {

        log.info("GET /api/v1/transactions/search?term={}", term);
        List<TransactionResponse> response = transactionService.searchTransactions(term);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transactions for a specific card
     */
    @GetMapping("/card/{cardNumber}")
    @Operation(
            summary = "List transactions for card",
            description = "Returns all transactions made with a specific credit card"
    )
    public ResponseEntity<List<TransactionResponse>> getTransactionsByCard(
            @Parameter(description = "Card number (16 digits)")
            @PathVariable String cardNumber) {

        log.info("GET /api/v1/transactions/card/****{}",
                cardNumber.length() > 4 ? cardNumber.substring(cardNumber.length() - 4) : "****");
        List<TransactionResponse> response = transactionService.getTransactionsByCard(cardNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction summary for an account (CORPT00C - Reports)
     */
    @GetMapping("/account/{accountId}/summary")
    @Operation(
            summary = "Get account transaction summary",
            description = "Returns aggregated statistics for an account. Replaces CICS CORPT00C transaction."
    )
    public ResponseEntity<TransactionSummaryResponse> getAccountSummary(
            @Parameter(description = "Account ID")
            @PathVariable Long accountId) {

        log.info("GET /api/v1/transactions/account/{}/summary", accountId);
        TransactionSummaryResponse response = transactionService.getTransactionSummary(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get overall transaction summary (Admin reports)
     */
    @GetMapping("/summary")
    @Operation(
            summary = "Get overall transaction summary",
            description = "Returns aggregated statistics across all transactions. Admin function."
    )
    public ResponseEntity<TransactionSummaryResponse> getOverallSummary() {

        log.info("GET /api/v1/transactions/summary");
        TransactionSummaryResponse response = transactionService.getOverallSummary();
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new transaction (COTRN02C - Transaction Add)
     */
    @PostMapping
    @Operation(
            summary = "Create transaction",
            description = "Creates a new transaction. Replaces CICS COTRN02C transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {

        log.info("POST /api/v1/transactions - type: {}, amount: {}",
                request.getTransactionType(), request.getAmount());
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
