package com.carddemo.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Transaction Service Application
 *
 * Microservice for transaction history and reporting.
 * Replaces CICS COTRN00C transaction.
 *
 * CICS to REST Mapping:
 *   CT00 (Transaction Browse)  -> GET /api/v1/transactions/account/{accountId}
 *   CT01 (Transaction View)    -> GET /api/v1/transactions/{transactionId}
 *   CORPT00C (Reports)         -> GET /api/v1/transactions/summary
 */
@SpringBootApplication
@EnableJpaAuditing
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
