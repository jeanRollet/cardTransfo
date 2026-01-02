package com.carddemo.notification.service;

import com.carddemo.notification.entity.WebhookDelivery;
import com.carddemo.notification.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Retry Scheduler Service
 *
 * Periodically checks for failed webhook deliveries and retries them.
 * Implements exponential backoff: 1min, 5min, 15min, 1h, 4h
 *
 * Replaces: CICS interval control for MQ retry patterns
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RetrySchedulerService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcher webhookDispatcher;

    /**
     * Process pending and failed deliveries every 30 seconds
     */
    @Scheduled(fixedDelay = 30000)
    public void processRetries() {
        List<WebhookDelivery> readyDeliveries = deliveryRepository.findReadyForRetry(
                WebhookDelivery.MAX_ATTEMPTS,
                LocalDateTime.now()
        );

        if (readyDeliveries.isEmpty()) {
            return;
        }

        log.info("Processing {} webhook deliveries ready for retry", readyDeliveries.size());

        for (WebhookDelivery delivery : readyDeliveries) {
            try {
                webhookDispatcher.deliver(delivery);
            } catch (Exception e) {
                log.error("Error processing delivery {}: {}",
                        delivery.getDeliveryId(), e.getMessage());
            }
        }
    }

    /**
     * Log statistics every 5 minutes
     */
    @Scheduled(fixedDelay = 300000)
    public void logStats() {
        long pending = deliveryRepository.countByStatus(WebhookDelivery.STATUS_PENDING);
        long failed = deliveryRepository.countByStatus(WebhookDelivery.STATUS_FAILED);
        long success = deliveryRepository.countByStatus(WebhookDelivery.STATUS_SUCCESS);
        long deadLetter = deliveryRepository.countByStatus(WebhookDelivery.STATUS_DEAD_LETTER);

        if (pending > 0 || failed > 0) {
            log.info("Webhook delivery stats: pending={}, failed={}, success={}, deadLetter={}",
                    pending, failed, success, deadLetter);
        }
    }
}
