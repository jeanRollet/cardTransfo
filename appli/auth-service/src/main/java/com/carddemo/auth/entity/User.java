package com.carddemo.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User Entity
 *
 * Maps to PostgreSQL 'users' table (from USRSEC VSAM file).
 *
 * COBOL Copybook Mapping (SEC-USER-DATA):
 *   SEC-USR-ID      PIC X(8)   -> userId
 *   SEC-USR-FNAME   PIC X(20)  -> firstName
 *   SEC-USR-LNAME   PIC X(20)  -> lastName
 *   SEC-USR-PWD     PIC X(8)   -> passwordHash (now BCrypt)
 *   SEC-USR-TYPE    PIC X(1)   -> userType
 *
 * @author CardDemo Transformation Team
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * User ID - Primary Key
     * Max 8 characters (from COBOL SEC-USR-ID)
     */
    @Id
    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    /**
     * First name (from SEC-USR-FNAME)
     */
    @Column(name = "first_name", length = 20, nullable = false)
    private String firstName;

    /**
     * Last name (from SEC-USR-LNAME)
     */
    @Column(name = "last_name", length = 20, nullable = false)
    private String lastName;

    /**
     * BCrypt password hash
     * Replaces plain-text SEC-USR-PWD from COBOL
     */
    @Column(name = "password_hash", length = 60, nullable = false)
    private String passwordHash;

    /**
     * User type: A=Admin, U=User (from SEC-USR-TYPE)
     */
    @Column(name = "user_type", columnDefinition = "char(1)", nullable = false)
    private String userType;

    /**
     * Customer ID - links regular users to their customer record
     * NULL for admin users (they can access all customers)
     */
    @Column(name = "customer_id")
    private Integer customerId;

    /**
     * Account active status
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Record creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Last successful login timestamp
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Failed login attempts counter
     */
    @Column(name = "login_attempts")
    @Builder.Default
    private Integer loginAttempts = 0;

    /**
     * Account lock timestamp (null = not locked)
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // Business methods

    /**
     * Check if user is an administrator
     */
    public boolean isAdmin() {
        return "A".equals(userType);
    }

    /**
     * Check if account is currently locked
     */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Increment login attempts and lock if threshold reached
     */
    public void incrementLoginAttempts(int maxAttempts, int lockDurationSeconds) {
        this.loginAttempts = (this.loginAttempts == null ? 0 : this.loginAttempts) + 1;
        if (this.loginAttempts >= maxAttempts) {
            this.lockedUntil = LocalDateTime.now().plusSeconds(lockDurationSeconds);
        }
    }

    /**
     * Reset login attempts on successful login
     */
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lockedUntil = null;
        this.lastLogin = LocalDateTime.now();
    }
}
