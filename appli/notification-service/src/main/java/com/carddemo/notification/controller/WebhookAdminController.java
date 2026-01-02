package com.carddemo.notification.controller;

import com.carddemo.notification.entity.WebhookDelivery;
import com.carddemo.notification.repository.WebhookDeliveryRepository;
import com.carddemo.notification.service.WebhookDeliveryService;
import com.carddemo.notification.service.WebhookDispatcher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Webhook Administration Controller
 *
 * Provides endpoints for monitoring and managing webhook deliveries.
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook Admin", description = "Webhook delivery monitoring and management")
public class WebhookAdminController {

    private final WebhookDeliveryService deliveryService;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcher webhookDispatcher;

    @GetMapping("/stats")
    @Operation(summary = "Get delivery statistics")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(deliveryService.getDeliveryStats());
    }

    @GetMapping("/deliveries")
    @Operation(summary = "Get recent deliveries")
    public ResponseEntity<List<WebhookDelivery>> getRecentDeliveries(
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(deliveryRepository.findRecentDeliveries(since));
    }

    @GetMapping("/deliveries/failed")
    @Operation(summary = "Get failed deliveries (dead letter)")
    public ResponseEntity<List<WebhookDelivery>> getFailedDeliveries() {
        return ResponseEntity.ok(
                deliveryRepository.findByStatusOrderByCreatedAtDesc(WebhookDelivery.STATUS_DEAD_LETTER));
    }

    @GetMapping("/deliveries/pending")
    @Operation(summary = "Get pending deliveries")
    public ResponseEntity<List<WebhookDelivery>> getPendingDeliveries() {
        return ResponseEntity.ok(deliveryRepository.findReadyForRetry(
                WebhookDelivery.MAX_ATTEMPTS,
                LocalDateTime.now().plusDays(1)));
    }

    @PostMapping("/deliveries/{deliveryId}/retry")
    @Operation(summary = "Manually retry a failed delivery")
    public ResponseEntity<Map<String, String>> retryDelivery(@PathVariable Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .map(delivery -> {
                    if (delivery.getStatus().equals(WebhookDelivery.STATUS_DEAD_LETTER)) {
                        // Reset for retry
                        delivery.setStatus(WebhookDelivery.STATUS_PENDING);
                        delivery.setAttemptCount(0);
                        delivery.setNextAttemptAt(LocalDateTime.now());
                        delivery.setCompletedAt(null);
                        deliveryRepository.save(delivery);

                        webhookDispatcher.deliver(delivery);

                        return ResponseEntity.ok(Map.of(
                                "message", "Delivery retry initiated",
                                "deliveryId", String.valueOf(deliveryId),
                                "status", delivery.getStatus()
                        ));
                    }
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Delivery is not in dead letter state"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/deliveries/partner/{partnerId}")
    @Operation(summary = "Get deliveries for a partner")
    public ResponseEntity<List<WebhookDelivery>> getPartnerDeliveries(@PathVariable Integer partnerId) {
        return ResponseEntity.ok(
                deliveryRepository.findByPartnerPartnerIdOrderByCreatedAtDesc(partnerId));
    }

    @GetMapping("/health")
    @Operation(summary = "Check webhook service health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Long> stats = deliveryService.getDeliveryStats();
        boolean healthy = stats.getOrDefault("pending", 0L) < 1000
                && stats.getOrDefault("failed", 0L) < 100;

        return ResponseEntity.ok(Map.of(
                "status", healthy ? "UP" : "DEGRADED",
                "stats", stats
        ));
    }
}
