package com.carddemo.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * CardDemo Authentication Service
 *
 * Replaces CICS transaction CC00 (COSGN00C program)
 * Provides JWT-based authentication for the CardDemo application
 *
 * z/OS CICS Equivalent:
 * - Transaction: CC00
 * - Program: COSGN00C (COBOL)
 * - Mapset: COSGN00 (BMS)
 * - File: USRSEC (VSAM KSDS)
 *
 * Cloud Native Implementation:
 * - REST API: POST /api/v1/auth/login
 * - Database: PostgreSQL (users table)
 * - Cache: Redis (session management)
 * - Security: JWT tokens + BCrypt password hashing
 *
 * @author CardDemo Transformation Team
 * @version 1.0.0
 * @since 2025-12-30
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
