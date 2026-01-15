package com.carddemo.transaction.service;

import com.carddemo.transaction.dto.AuthorizationStatsResponse;
import com.carddemo.transaction.dto.PendingAuthorizationResponse;
import com.carddemo.transaction.entity.PendingAuthorization;
import com.carddemo.transaction.exception.TransactionException;
import com.carddemo.transaction.repository.PendingAuthorizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pending Authorization Service
 *
 * Handles pending authorization operations including approval, decline, and fraud reporting.
 * Replaces CICS COAUTH0C transaction functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PendingAuthorizationService {

    private final PendingAuthorizationRepository authorizationRepository;

    public List<PendingAuthorizationResponse> getAllPending() {
        log.info("Getting all pending authorizations");
        return authorizationRepository.findByStatusOrderByAuthTimestampDesc("PENDING")
                .stream()
                .map(PendingAuthorizationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PendingAuthorizationResponse> getPendingByAccount(Long accountId) {
        log.info("Getting pending authorizations for account: {}", accountId);
        return authorizationRepository.findByAccountIdAndStatusOrderByAuthTimestampDesc(accountId, "PENDING")
                .stream()
                .map(PendingAuthorizationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<PendingAuthorizationResponse> getPendingByCustomer(Integer customerId) {
        log.info("Getting pending authorizations for customer: {}", customerId);
        // For now, just return all pending - in production this would filter by customer's accounts
        return getAllPending();
    }

    public List<PendingAuthorizationResponse> getFraudAlerts() {
        log.info("Getting fraud alerts");
        return authorizationRepository.findByIsFraudAlertTrueAndStatusOrderByAuthTimestampDesc("PENDING")
                .stream()
                .map(PendingAuthorizationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AuthorizationStatsResponse getStats() {
        log.info("Getting authorization stats");
        long totalPending = authorizationRepository.countPending();
        BigDecimal totalAmount = authorizationRepository.sumPendingAmount();
        long fraudAlerts = authorizationRepository.countFraudAlerts();
        long highRisk = authorizationRepository.countHighRisk();

        return AuthorizationStatsResponse.builder()
                .totalPending(totalPending)
                .totalAmount(totalAmount)
                .totalAmountFormatted(String.format("$%.2f", totalAmount))
                .fraudAlerts(fraudAlerts)
                .highRiskCount(highRisk)
                .build();
    }

    public AuthorizationStatsResponse getStatsByCustomer(Integer customerId) {
        log.info("Getting authorization stats for customer: {}", customerId);
        // For now, return overall stats - in production this would filter by customer
        return getStats();
    }

    @Transactional
    public PendingAuthorizationResponse approveAuthorization(Long authId) {
        log.info("Approving authorization: {}", authId);
        PendingAuthorization auth = authorizationRepository.findById(authId)
                .orElseThrow(() -> new TransactionException("Authorization not found: " + authId));

        if (!"PENDING".equals(auth.getStatus())) {
            throw new TransactionException("Cannot approve authorization with status: " + auth.getStatus());
        }

        if (auth.getIsFraudAlert()) {
            throw new TransactionException("Cannot approve fraud-flagged authorization. Use report-fraud endpoint.");
        }

        auth.setStatus("APPROVED");
        auth.setSettledAt(LocalDateTime.now());
        auth = authorizationRepository.save(auth);

        log.info("Authorization {} approved", authId);
        return PendingAuthorizationResponse.fromEntity(auth);
    }

    @Transactional
    public PendingAuthorizationResponse declineAuthorization(Long authId, String reason) {
        log.info("Declining authorization: {} with reason: {}", authId, reason);
        PendingAuthorization auth = authorizationRepository.findById(authId)
                .orElseThrow(() -> new TransactionException("Authorization not found: " + authId));

        if (!"PENDING".equals(auth.getStatus())) {
            throw new TransactionException("Cannot decline authorization with status: " + auth.getStatus());
        }

        auth.setStatus("DECLINED");
        auth.setDeclineReason(reason);
        auth.setSettledAt(LocalDateTime.now());
        auth = authorizationRepository.save(auth);

        log.info("Authorization {} declined", authId);
        return PendingAuthorizationResponse.fromEntity(auth);
    }

    @Transactional
    public PendingAuthorizationResponse reportFraud(Long authId) {
        log.info("Reporting fraud for authorization: {}", authId);
        PendingAuthorization auth = authorizationRepository.findById(authId)
                .orElseThrow(() -> new TransactionException("Authorization not found: " + authId));

        auth.setStatus("DECLINED");
        auth.setDeclineReason("FRAUD - Card blocked");
        auth.setIsFraudAlert(true);
        auth.setRiskScore(100);
        auth.setSettledAt(LocalDateTime.now());
        auth = authorizationRepository.save(auth);

        // In production, this would also trigger card blocking via card-service
        log.info("Fraud reported for authorization {} - card should be blocked", authId);
        return PendingAuthorizationResponse.fromEntity(auth);
    }
}
