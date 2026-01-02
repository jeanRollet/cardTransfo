package com.carddemo.card;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Card Service Application
 *
 * Replaces COCRDLIC (Card List) and COCRDSLC (Card Select) CICS transactions.
 * Provides REST API for credit card management operations.
 *
 * Event Publishing:
 *   Publishes CardStatusChangedEvent to Kafka (carddemo.cards)
 *   Uses Outbox Pattern for reliable event publishing
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class CardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardServiceApplication.class, args);
    }
}
