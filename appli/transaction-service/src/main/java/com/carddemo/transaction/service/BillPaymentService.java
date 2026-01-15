package com.carddemo.transaction.service;

import com.carddemo.transaction.dto.*;
import com.carddemo.transaction.entity.BillPayee;
import com.carddemo.transaction.entity.BillPayment;
import com.carddemo.transaction.exception.TransactionException;
import com.carddemo.transaction.repository.BillPayeeRepository;
import com.carddemo.transaction.repository.BillPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bill Payment Service
 *
 * Handles bill payment operations including payee management and payment processing.
 * Replaces CICS COBIL00C transaction functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillPaymentService {

    private final BillPayeeRepository payeeRepository;
    private final BillPaymentRepository paymentRepository;

    // ==================== Payee Operations ====================

    public List<BillPayeeResponse> getPayeesByCustomer(Integer customerId) {
        log.info("Getting payees for customer: {}", customerId);
        return payeeRepository.findByCustomerIdAndIsActiveTrueOrderByPayeeNameAsc(customerId)
                .stream()
                .map(BillPayeeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public BillPayeeResponse createPayee(CreatePayeeRequest request) {
        log.info("Creating payee for customer: {}", request.getCustomerId());

        BillPayee payee = BillPayee.builder()
                .customerId(request.getCustomerId())
                .payeeName(request.getPayeeName())
                .payeeType(request.getPayeeType())
                .payeeAccountNumber(request.getPayeeAccountNumber())
                .nickname(request.getNickname())
                .isActive(true)
                .build();

        payee = payeeRepository.save(payee);
        log.info("Created payee with ID: {}", payee.getPayeeId());

        return BillPayeeResponse.fromEntity(payee);
    }

    @Transactional
    public void deletePayee(Long payeeId) {
        log.info("Deleting payee: {}", payeeId);
        BillPayee payee = payeeRepository.findById(payeeId)
                .orElseThrow(() -> new TransactionException("Payee not found: " + payeeId));

        // Soft delete
        payee.setIsActive(false);
        payeeRepository.save(payee);
        log.info("Payee {} marked as inactive", payeeId);
    }

    // ==================== Payment Operations ====================

    public List<BillPaymentResponse> getPaymentsByAccount(Long accountId) {
        log.info("Getting payments for account: {}", accountId);
        return paymentRepository.findByAccountIdOrderByPaymentDateDesc(accountId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    public List<BillPaymentResponse> getPaymentsByCustomer(Integer customerId) {
        log.info("Getting payments for customer: {}", customerId);
        return paymentRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    public List<BillPaymentResponse> getScheduledPaymentsByCustomer(Integer customerId) {
        log.info("Getting scheduled payments for customer: {}", customerId);
        return paymentRepository.findScheduledByCustomerId(customerId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BillPaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment from account {} to payee {}", request.getAccountId(), request.getPayeeId());

        // Verify payee exists
        BillPayee payee = payeeRepository.findById(request.getPayeeId())
                .orElseThrow(() -> new TransactionException("Payee not found: " + request.getPayeeId()));

        String status = request.getPaymentDate().isAfter(LocalDate.now()) ? "SCHEDULED" : "COMPLETED";
        String confirmationNumber = generateConfirmationNumber();

        BillPayment payment = BillPayment.builder()
                .accountId(request.getAccountId())
                .payeeId(request.getPayeeId())
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .scheduledDate(request.getPaymentDate().isAfter(LocalDate.now()) ? request.getPaymentDate() : null)
                .status(status)
                .confirmationNumber(confirmationNumber)
                .memo(request.getMemo())
                .isRecurring(request.getIsRecurring() != null && request.getIsRecurring())
                .recurringFrequency(request.getRecurringFrequency())
                .build();

        // Calculate next payment date for recurring payments
        if (Boolean.TRUE.equals(payment.getIsRecurring()) && payment.getRecurringFrequency() != null) {
            payment.setNextPaymentDate(calculateNextPaymentDate(request.getPaymentDate(), payment.getRecurringFrequency()));
        }

        payment = paymentRepository.save(payment);
        log.info("Created payment with ID: {} and confirmation: {}", payment.getPaymentId(), confirmationNumber);

        return BillPaymentResponse.fromEntity(payment, payee.getPayeeName());
    }

    @Transactional
    public void cancelPayment(Long paymentId) {
        log.info("Cancelling payment: {}", paymentId);
        BillPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new TransactionException("Payment not found: " + paymentId));

        if (!"SCHEDULED".equals(payment.getStatus()) && !"PENDING".equals(payment.getStatus())) {
            throw new TransactionException("Cannot cancel payment with status: " + payment.getStatus());
        }

        payment.setStatus("CANCELLED");
        paymentRepository.save(payment);
        log.info("Payment {} cancelled", paymentId);
    }

    // ==================== Helper Methods ====================

    private BillPaymentResponse toPaymentResponse(BillPayment payment) {
        String payeeName = payeeRepository.findById(payment.getPayeeId())
                .map(BillPayee::getPayeeName)
                .orElse("Unknown");
        return BillPaymentResponse.fromEntity(payment, payeeName);
    }

    private String generateConfirmationNumber() {
        return "BP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private LocalDate calculateNextPaymentDate(LocalDate currentDate, String frequency) {
        return switch (frequency) {
            case "WEEKLY" -> currentDate.plusWeeks(1);
            case "BIWEEKLY" -> currentDate.plusWeeks(2);
            case "MONTHLY" -> currentDate.plusMonths(1);
            case "QUARTERLY" -> currentDate.plusMonths(3);
            case "ANNUALLY" -> currentDate.plusYears(1);
            default -> currentDate.plusMonths(1);
        };
    }
}
