package com.carddemo.partner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Partner API Gateway Service
 *
 * Provides API Key authentication, rate limiting, and partner management.
 * Replaces CICS Web Services / MQ Series for partner integrations.
 *
 * Port: 8085
 */
@SpringBootApplication
public class PartnerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PartnerServiceApplication.class, args);
    }
}
