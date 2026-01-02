package com.carddemo.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service Application
 *
 * This service replaces MQ Series consumer patterns from the mainframe.
 * It consumes events from Kafka topics and delivers webhooks to partners.
 *
 * Features:
 * - Kafka consumers for transaction, card, and account events
 * - Webhook delivery with exponential backoff retry
 * - Partner subscription management
 * - Scope-based event filtering
 *
 * Equivalent mainframe programs replaced:
 * - MQ GET message handlers
 * - CICS Web Services callback routines
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
