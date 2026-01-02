package com.carddemo.notification.service;

import com.carddemo.notification.entity.Partner;
import com.carddemo.notification.entity.WebhookDelivery;
import com.carddemo.notification.entity.WebhookSubscription;
import com.carddemo.notification.repository.PartnerRepository;
import com.carddemo.notification.repository.WebhookDeliveryRepository;
import com.carddemo.notification.repository.WebhookSubscriptionRepository;
import com.carddemo.shared.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Webhook Delivery Service
 *
 * Manages the creation of webhook delivery records based on partner subscriptions.
 * Replaces: CICS Web Services routing logic
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final PartnerRepository partnerRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create webhook deliveries for all subscribed partners
     */
    @Transactional
    public int createDeliveriesForEvent(DomainEvent event) {
        String eventType = event.getEventType();
        String requiredScope = event.getRequiredScope();

        log.debug("Creating deliveries for event: {} (eventId: {})", eventType, event.getEventId());

        // Find partners with active subscriptions for this event type
        List<WebhookSubscription> subscriptions = subscriptionRepository
                .findActiveSubscriptionsByEventType(eventType);

        int deliveriesCreated = 0;

        for (WebhookSubscription subscription : subscriptions) {
            Partner partner = subscription.getPartner();

            // Verify partner has the required scope
            if (!partner.hasScope(requiredScope)) {
                log.debug("Partner {} does not have scope {} for event {}",
                        partner.getPartnerName(), requiredScope, eventType);
                continue;
            }

            // Create delivery record
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = objectMapper.convertValue(event, Map.class);

                WebhookDelivery delivery = WebhookDelivery.builder()
                        .eventId(event.getEventId())
                        .partner(partner)
                        .webhookUrl(partner.getWebhookUrl())
                        .payload(payload)
                        .build();

                deliveryRepository.save(delivery);
                deliveriesCreated++;

                log.info("Created webhook delivery for partner {} (event: {}, eventId: {})",
                        partner.getPartnerName(), eventType, event.getEventId());

            } catch (Exception e) {
                log.error("Failed to create delivery for partner {}: {}",
                        partner.getPartnerName(), e.getMessage());
            }
        }

        // Fallback: If no subscriptions, check partners with matching scope
        if (subscriptions.isEmpty()) {
            log.debug("No subscriptions found, checking partners with scope: {}", requiredScope);
            deliveriesCreated = createDeliveriesForScopedPartners(event, requiredScope);
        }

        return deliveriesCreated;
    }

    /**
     * Create deliveries for partners that have the required scope (fallback)
     */
    private int createDeliveriesForScopedPartners(DomainEvent event, String requiredScope) {
        List<Partner> partners = partnerRepository.findByActiveAndHasScope(requiredScope);
        int deliveriesCreated = 0;

        for (Partner partner : partners) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = objectMapper.convertValue(event, Map.class);

                WebhookDelivery delivery = WebhookDelivery.builder()
                        .eventId(event.getEventId())
                        .partner(partner)
                        .webhookUrl(partner.getWebhookUrl())
                        .payload(payload)
                        .build();

                deliveryRepository.save(delivery);
                deliveriesCreated++;

                log.info("Created webhook delivery for scoped partner {} (event: {})",
                        partner.getPartnerName(), event.getEventType());

            } catch (Exception e) {
                log.error("Failed to create delivery for partner {}: {}",
                        partner.getPartnerName(), e.getMessage());
            }
        }

        return deliveriesCreated;
    }

    /**
     * Get delivery statistics
     */
    public Map<String, Long> getDeliveryStats() {
        return Map.of(
                "pending", deliveryRepository.countByStatus(WebhookDelivery.STATUS_PENDING),
                "failed", deliveryRepository.countByStatus(WebhookDelivery.STATUS_FAILED),
                "success", deliveryRepository.countByStatus(WebhookDelivery.STATUS_SUCCESS),
                "deadLetter", deliveryRepository.countByStatus(WebhookDelivery.STATUS_DEAD_LETTER)
        );
    }
}
