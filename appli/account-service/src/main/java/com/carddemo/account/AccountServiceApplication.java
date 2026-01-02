package com.carddemo.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Account Management Service - replaces COACTVWC CICS transaction
 * Provides REST APIs for viewing and managing credit card accounts
 *
 * Event Publishing:
 *   Publishes AccountUpdatedEvent to Kafka (carddemo.accounts)
 *   Uses Outbox Pattern for reliable event publishing
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
