package com.carddemo.card;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Card Service Application
 *
 * Replaces COCRDLIC (Card List) and COCRDSLC (Card Select) CICS transactions.
 * Provides REST API for credit card management operations.
 */
@SpringBootApplication
@EnableJpaAuditing
public class CardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardServiceApplication.class, args);
    }
}
