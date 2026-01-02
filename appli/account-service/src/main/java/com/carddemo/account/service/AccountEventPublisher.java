package com.carddemo.account.service;

import com.carddemo.account.entity.Account;
import com.carddemo.account.entity.AccountOutbox;
import com.carddemo.account.repository.AccountOutboxRepository;
import com.carddemo.shared.event.AccountUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Account Event Publisher
 *
 * Publishes account events using the Outbox Pattern.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventPublisher {

    private final AccountOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publish status change event
     */
    @Transactional
    public void publishStatusChanged(Account account, String previousStatus, String changedBy) {
        AccountUpdatedEvent event = AccountUpdatedEvent.createStatusChange(
                account.getAccountId(),
                account.getCustomerId(),
                previousStatus,
                account.getActiveStatus(),
                changedBy
        );
        saveToOutbox(event);
    }

    /**
     * Publish balance change event
     */
    @Transactional
    public void publishBalanceChanged(Account account, BigDecimal previousBalance, String changedBy) {
        AccountUpdatedEvent event = AccountUpdatedEvent.createBalanceChange(
                account.getAccountId(),
                account.getCustomerId(),
                previousBalance,
                account.getCurrentBalance(),
                changedBy
        );
        saveToOutbox(event);
    }

    /**
     * Publish credit limit change event
     */
    @Transactional
    public void publishLimitChanged(Account account, BigDecimal previousLimit, String changedBy) {
        AccountUpdatedEvent event = AccountUpdatedEvent.createLimitChange(
                account.getAccountId(),
                account.getCustomerId(),
                previousLimit,
                account.getCreditLimit(),
                changedBy
        );
        saveToOutbox(event);
    }

    private void saveToOutbox(AccountUpdatedEvent event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);

            AccountOutbox outbox = AccountOutbox.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .aggregateId(String.valueOf(event.getAccountId()))
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);

            log.info("Account event saved to outbox: eventId={}, type={}, updateType={}, accountId={}",
                    event.getEventId(), event.getEventType(), event.getUpdateType(), event.getAccountId());

        } catch (Exception e) {
            log.error("Failed to save account event to outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish account event", e);
        }
    }
}
