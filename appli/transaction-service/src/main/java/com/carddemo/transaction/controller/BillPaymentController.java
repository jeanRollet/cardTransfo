package com.carddemo.transaction.controller;

import com.carddemo.transaction.dto.*;
import com.carddemo.transaction.service.BillPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bill Payment Controller
 *
 * REST API for bill payment operations.
 *
 * CICS Transaction to REST Mapping:
 *   COBIL00C (Bill Payment) -> POST /api/v1/bill-payments
 *   COBIL00C (Payee List)   -> GET /api/v1/bill-payments/payees/customer/{customerId}
 */
@RestController
@RequestMapping("/api/v1/bill-payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bill Payment", description = "Bill payment and payee management (COBIL00C)")
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    // ==================== Payee Endpoints ====================

    @GetMapping("/payees/customer/{customerId}")
    @Operation(
            summary = "List payees for customer",
            description = "Returns all active payees for the specified customer."
    )
    public ResponseEntity<List<BillPayeeResponse>> getPayeesByCustomer(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/bill-payments/payees/customer/{}", customerId);
        List<BillPayeeResponse> response = billPaymentService.getPayeesByCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payees")
    @Operation(
            summary = "Create payee",
            description = "Creates a new bill payee for the customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payee created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<BillPayeeResponse> createPayee(
            @Valid @RequestBody CreatePayeeRequest request) {

        log.info("POST /api/v1/bill-payments/payees - customer: {}", request.getCustomerId());
        BillPayeeResponse response = billPaymentService.createPayee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/payees/{payeeId}")
    @Operation(
            summary = "Delete payee",
            description = "Deletes (deactivates) a bill payee."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payee deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Payee not found")
    })
    public ResponseEntity<Void> deletePayee(
            @Parameter(description = "Payee ID")
            @PathVariable Long payeeId) {

        log.info("DELETE /api/v1/bill-payments/payees/{}", payeeId);
        billPaymentService.deletePayee(payeeId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Payment Endpoints ====================

    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "List payments for account",
            description = "Returns payment history for the specified account."
    )
    public ResponseEntity<List<BillPaymentResponse>> getPaymentsByAccount(
            @Parameter(description = "Account ID")
            @PathVariable Long accountId) {

        log.info("GET /api/v1/bill-payments/account/{}", accountId);
        List<BillPaymentResponse> response = billPaymentService.getPaymentsByAccount(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "List payments for customer",
            description = "Returns payment history for the specified customer across all accounts."
    )
    public ResponseEntity<List<BillPaymentResponse>> getPaymentsByCustomer(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/bill-payments/customer/{}", customerId);
        List<BillPaymentResponse> response = billPaymentService.getPaymentsByCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/scheduled/customer/{customerId}")
    @Operation(
            summary = "List scheduled payments for customer",
            description = "Returns pending/scheduled payments for the customer."
    )
    public ResponseEntity<List<BillPaymentResponse>> getScheduledPayments(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId) {

        log.info("GET /api/v1/bill-payments/scheduled/customer/{}", customerId);
        List<BillPaymentResponse> response = billPaymentService.getScheduledPaymentsByCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(
            summary = "Create payment",
            description = "Creates a new bill payment. Replaces CICS COBIL00C transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Payee not found")
    })
    public ResponseEntity<BillPaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        log.info("POST /api/v1/bill-payments - account: {}, payee: {}, amount: {}",
                request.getAccountId(), request.getPayeeId(), request.getAmount());
        BillPaymentResponse response = billPaymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(
            summary = "Cancel payment",
            description = "Cancels a scheduled or pending payment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payment cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Payment cannot be cancelled"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<Void> cancelPayment(
            @Parameter(description = "Payment ID")
            @PathVariable Long paymentId) {

        log.info("POST /api/v1/bill-payments/{}/cancel", paymentId);
        billPaymentService.cancelPayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}
