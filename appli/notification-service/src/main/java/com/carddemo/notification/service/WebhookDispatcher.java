package com.carddemo.notification.service;

import com.carddemo.notification.entity.WebhookDelivery;
import com.carddemo.notification.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Webhook Dispatcher
 *
 * Handles the actual HTTP delivery of webhooks to partner endpoints.
 * Includes exponential backoff retry logic.
 *
 * Security: Uses X-Webhook-Secret header for authentication.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final WebClient webClient;
    private final WebhookDeliveryRepository deliveryRepository;

    @Value("${webhook.secret:carddemo-webhook-secret}")
    private String webhookSecret;

    @Value("${webhook.timeout.delivery:10000}")
    private int deliveryTimeoutMs;

    /**
     * Attempt to deliver a webhook
     */
    @Transactional
    public void deliver(WebhookDelivery delivery) {
        log.info("Attempting webhook delivery {} to {} (attempt {})",
                delivery.getDeliveryId(),
                delivery.getWebhookUrl(),
                delivery.getAttemptCount() + 1);

        try {
            webClient.post()
                    .uri(delivery.getWebhookUrl())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Webhook-Secret", webhookSecret)
                    .header("X-Event-Id", delivery.getEventId().toString())
                    .header("X-Delivery-Id", String.valueOf(delivery.getDeliveryId()))
                    .bodyValue(delivery.getPayload())
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(deliveryTimeoutMs))
                    .doOnSuccess(response -> handleSuccess(delivery))
                    .doOnError(error -> handleError(delivery, error))
                    .onErrorResume(e -> Mono.empty())
                    .block();

        } catch (Exception e) {
            handleError(delivery, e);
        }
    }

    /**
     * Handle successful delivery
     */
    private void handleSuccess(WebhookDelivery delivery) {
        log.info("Webhook delivered successfully: deliveryId={}, partner={}",
                delivery.getDeliveryId(),
                delivery.getPartner().getPartnerName());

        delivery.markSuccess();
        deliveryRepository.save(delivery);
    }

    /**
     * Handle delivery error
     */
    private void handleError(WebhookDelivery delivery, Throwable error) {
        String errorMessage;

        if (error instanceof WebClientResponseException wcre) {
            HttpStatusCode status = wcre.getStatusCode();
            errorMessage = String.format("HTTP %d: %s", status.value(), wcre.getStatusText());
            log.warn("Webhook delivery failed with HTTP {}: deliveryId={}, url={}",
                    status.value(), delivery.getDeliveryId(), delivery.getWebhookUrl());
        } else {
            errorMessage = error.getClass().getSimpleName() + ": " + error.getMessage();
            log.warn("Webhook delivery failed: deliveryId={}, error={}",
                    delivery.getDeliveryId(), errorMessage);
        }

        delivery.markFailed(errorMessage);
        deliveryRepository.save(delivery);

        if (delivery.getStatus().equals(WebhookDelivery.STATUS_DEAD_LETTER)) {
            log.error("Webhook delivery exhausted all retries, moved to dead letter: " +
                            "deliveryId={}, partner={}, eventId={}",
                    delivery.getDeliveryId(),
                    delivery.getPartner().getPartnerName(),
                    delivery.getEventId());
        } else {
            log.info("Webhook delivery scheduled for retry at {}: deliveryId={}",
                    delivery.getNextAttemptAt(), delivery.getDeliveryId());
        }
    }

    /**
     * Deliver immediately (for newly created deliveries)
     */
    @Transactional
    public void deliverImmediately(WebhookDelivery delivery) {
        deliver(delivery);
    }
}
