package com.carddemo.account.controller;

import com.carddemo.account.dto.AccountResponse;
import com.carddemo.account.dto.AccountSummaryResponse;
import com.carddemo.account.dto.CustomerResponse;
import com.carddemo.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Account Controller - REST API for COACTVWC Account View
 * Replaces CICS transaction COACTVWC with RESTful endpoints
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account management APIs (replaces COACTVWC)")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Get all accounts with pagination
     * Replaces: CICS STARTBR ACCTDAT / READNEXT loop
     */
    @GetMapping
    @Operation(summary = "List all accounts", description = "Get paginated list of all accounts")
    public ResponseEntity<Page<AccountResponse>> getAllAccounts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/accounts - page={}, size={}", page, size);
        Page<AccountResponse> accounts = accountService.getAllAccounts(page, size);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get account by ID
     * Replaces: CICS READ ACCTDAT with RIDFLD(account-id)
     */
    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details", description = "Get detailed information for a specific account")
    public ResponseEntity<AccountResponse> getAccountById(
            @Parameter(description = "Account ID")
            @PathVariable Long accountId) {

        log.info("GET /api/v1/accounts/{}", accountId);
        AccountResponse account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(account);
    }

    /**
     * Get accounts by customer ID - main COACTVWC function
     * Replaces: CICS READ ACCTDAT with alternate index on customer-id
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get accounts by customer", description = "Get all accounts for a specific customer (COACTVWC)")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomer(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/accounts/customer/{}", customerId);
        List<AccountResponse> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Search accounts by customer name
     */
    @GetMapping("/search")
    @Operation(summary = "Search accounts", description = "Search accounts by customer name")
    public ResponseEntity<List<AccountResponse>> searchAccounts(
            @Parameter(description = "Customer name to search")
            @RequestParam String name) {

        log.info("GET /api/v1/accounts/search?name={}", name);
        List<AccountResponse> accounts = accountService.searchByCustomerName(name);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get expiring accounts
     */
    @GetMapping("/expiring")
    @Operation(summary = "Get expiring accounts", description = "Get accounts expiring within specified days")
    public ResponseEntity<List<AccountResponse>> getExpiringAccounts(
            @Parameter(description = "Number of days")
            @RequestParam(defaultValue = "30") int days) {

        log.info("GET /api/v1/accounts/expiring?days={}", days);
        List<AccountResponse> accounts = accountService.getExpiringAccounts(days);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get high utilization accounts
     */
    @GetMapping("/high-utilization")
    @Operation(summary = "Get high utilization accounts", description = "Get accounts with utilization above threshold")
    public ResponseEntity<List<AccountResponse>> getHighUtilizationAccounts(
            @Parameter(description = "Utilization threshold percentage")
            @RequestParam(defaultValue = "80") BigDecimal threshold) {

        log.info("GET /api/v1/accounts/high-utilization?threshold={}", threshold);
        List<AccountResponse> accounts = accountService.getHighUtilizationAccounts(threshold);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get account summary/statistics
     */
    @GetMapping("/summary")
    @Operation(summary = "Get account summary", description = "Get aggregate statistics for all accounts")
    public ResponseEntity<AccountSummaryResponse> getAccountSummary() {
        log.info("GET /api/v1/accounts/summary");
        AccountSummaryResponse summary = accountService.getAccountSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Update account status
     * Replaces: CICS REWRITE ACCTDAT
     */
    @PutMapping("/{accountId}/status")
    @Operation(summary = "Update account status", description = "Update account active status (Y=Active, N=Closed)")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @Parameter(description = "Account ID")
            @PathVariable Long accountId,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        log.info("PUT /api/v1/accounts/{}/status - status={}", accountId, status);
        AccountResponse account = accountService.updateAccountStatus(accountId, status);
        return ResponseEntity.ok(account);
    }

    // ========== Customer Endpoints ==========

    /**
     * Get customer by ID
     */
    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Get customer details", description = "Get detailed information for a specific customer")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/accounts/customers/{}", customerId);
        CustomerResponse customer = accountService.getCustomerById(customerId);
        return ResponseEntity.ok(customer);
    }

    /**
     * Get all customers with pagination
     */
    @GetMapping("/customers")
    @Operation(summary = "List all customers", description = "Get paginated list of all customers")
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/accounts/customers - page={}, size={}", page, size);
        Page<CustomerResponse> customers = accountService.getAllCustomers(page, size);
        return ResponseEntity.ok(customers);
    }

    /**
     * Search customers by name
     */
    @GetMapping("/customers/search")
    @Operation(summary = "Search customers", description = "Search customers by name")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(
            @Parameter(description = "Customer name to search")
            @RequestParam String name) {

        log.info("GET /api/v1/accounts/customers/search?name={}", name);
        List<CustomerResponse> customers = accountService.searchCustomersByName(name);
        return ResponseEntity.ok(customers);
    }
}
