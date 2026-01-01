package com.carddemo.auth.repository;

import com.carddemo.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * User Repository
 *
 * Replaces VSAM USRSEC file access.
 *
 * CICS Equivalent:
 *   EXEC CICS READ FILE('USRSEC')
 *                   INTO(SEC-USER-DATA)
 *                   RIDFLD(WS-USER-ID)
 *   END-EXEC.
 *
 * @author CardDemo Transformation Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find active user by user ID
     *
     * COBOL equivalent:
     *   READ USRSEC into SEC-USER-DATA
     *   IF SEC-USR-ID = WS-USER-ID AND SEC-ACTIVE = 'Y'
     */
    Optional<User> findByUserIdAndIsActiveTrue(String userId);

    /**
     * Find user by ID (any status)
     */
    Optional<User> findByUserId(String userId);

    /**
     * Check if user exists
     */
    boolean existsByUserId(String userId);

    /**
     * Update last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :timestamp, u.loginAttempts = 0, u.lockedUntil = null WHERE u.userId = :userId")
    int updateLastLogin(@Param("userId") String userId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Increment login attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.userId = :userId")
    int incrementLoginAttempts(@Param("userId") String userId);

    /**
     * Lock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil WHERE u.userId = :userId")
    int lockAccount(@Param("userId") String userId, @Param("lockedUntil") LocalDateTime lockedUntil);

    /**
     * Unlock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = null, u.loginAttempts = 0 WHERE u.userId = :userId")
    int unlockAccount(@Param("userId") String userId);
}
