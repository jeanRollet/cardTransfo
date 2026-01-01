package com.carddemo.auth.service;

import com.carddemo.auth.entity.User;
import com.carddemo.auth.exception.AuthException;
import com.carddemo.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication Service
 *
 * Core business logic for user authentication.
 * Replaces COSGN00C COBOL program logic.
 *
 * CICS Flow (Original):
 * 1. RECEIVE MAP (get user input)
 * 2. READ USRSEC FILE with user-id
 * 3. Compare password (plain text in COBOL)
 * 4. If OK: XCTL to COMEN01C with COMMAREA
 * 5. If KO: Display error message
 *
 * Cloud Native Flow:
 * 1. Receive JSON credentials
 * 2. Query PostgreSQL users table
 * 3. Verify BCrypt password hash
 * 4. Generate JWT token pair
 * 5. Store session in Redis
 * 6. Return tokens + user info
 *
 * @author CardDemo Transformation Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${auth.lock-duration-seconds:1800}")
    private int lockDurationSeconds;

    /**
     * Authenticate user and generate tokens
     *
     * @param userId   User ID (max 8 chars, from COBOL SEC-USR-ID)
     * @param password Plain text password
     * @return AuthResult with tokens and user info
     * @throws AuthException if authentication fails
     */
    @Transactional
    public AuthResult authenticate(String userId, String password) {
        log.info("Authentication attempt for user: {}", userId);

        // Step 1: Find user in database
        // CICS equivalent: EXEC CICS READ FILE('USRSEC') INTO(SEC-USER-DATA) RIDFLD(WS-USER-ID)
        User user = userRepository.findByUserId(userId.toUpperCase())
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return AuthException.invalidCredentials(); // Don't reveal user existence
                });

        // Step 2: Check if account is active
        if (!user.getIsActive()) {
            log.warn("Disabled account login attempt: {}", userId);
            throw AuthException.accountDisabled();
        }

        // Step 3: Check if account is locked
        if (user.isLocked()) {
            log.warn("Locked account login attempt: {}", userId);
            throw AuthException.accountLocked();
        }

        // Step 4: Verify password
        // CICS equivalent: IF SEC-USR-PWD = WS-PASSWORD
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            handleFailedLogin(user);
            throw AuthException.invalidCredentials();
        }

        // Step 5: Successful authentication - reset attempts and update last login
        user.resetLoginAttempts();
        userRepository.save(user);

        // Step 6: Generate session ID and tokens
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(user, sessionId);
        String refreshToken = jwtService.generateRefreshToken(user.getUserId(), sessionId);

        log.info("Authentication successful for user: {}", userId);

        return new AuthResult(accessToken, refreshToken, user, sessionId);
    }

    /**
     * Handle failed login attempt
     */
    private void handleFailedLogin(User user) {
        user.incrementLoginAttempts(maxLoginAttempts, lockDurationSeconds);
        userRepository.save(user);

        if (user.isLocked()) {
            log.warn("Account locked after {} failed attempts: {}",
                    maxLoginAttempts, user.getUserId());
        } else {
            log.warn("Failed login attempt {} of {} for user: {}",
                    user.getLoginAttempts(), maxLoginAttempts, user.getUserId());
        }
    }

    /**
     * Refresh access token using refresh token
     *
     * @param refreshToken Valid refresh token
     * @return New access token
     * @throws AuthException if refresh token is invalid
     */
    @Transactional(readOnly = true)
    public RefreshResult refreshToken(String refreshToken) {
        log.debug("Token refresh attempt");

        // Validate refresh token
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw AuthException.refreshTokenInvalid();
        }

        // Extract user info from refresh token
        String userId = jwtService.extractUserId(refreshToken);
        String sessionId = jwtService.extractSessionId(refreshToken);

        // Verify user still exists and is active
        User user = userRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(AuthException::accountDisabled);

        // Generate new access token (same session)
        String newAccessToken = jwtService.generateAccessToken(user, sessionId);

        log.debug("Token refreshed for user: {}", userId);

        return new RefreshResult(newAccessToken);
    }

    /**
     * Logout user - invalidate tokens
     *
     * @param accessToken JWT access token to invalidate
     */
    public void logout(String accessToken) {
        String token = jwtService.extractTokenFromHeader(accessToken);

        if (token != null) {
            String userId = null;
            try {
                userId = jwtService.extractUserId(token);
            } catch (Exception e) {
                // Token might be invalid, still try to blacklist
            }

            jwtService.blacklistToken(token);
            log.info("User logged out: {}", userId);
        }
    }

    /**
     * Validate token and return user info
     *
     * @param accessToken JWT access token
     * @return TokenValidationResult with validity and user info
     */
    public TokenValidationResult validateToken(String accessToken) {
        String token = jwtService.extractTokenFromHeader(accessToken);

        if (token == null || !jwtService.validateToken(token)) {
            return new TokenValidationResult(false, null, null, null);
        }

        try {
            String userId = jwtService.extractUserId(token);
            String userType = jwtService.extractUserType(token);
            String sessionId = jwtService.extractSessionId(token);

            return new TokenValidationResult(true, userId, userType, sessionId);
        } catch (Exception e) {
            return new TokenValidationResult(false, null, null, null);
        }
    }

    // ============================================================================
    // Result Classes
    // ============================================================================

    /**
     * Authentication result
     */
    public record AuthResult(
            String accessToken,
            String refreshToken,
            User user,
            String sessionId
    ) {}

    /**
     * Token refresh result
     */
    public record RefreshResult(String accessToken) {}

    /**
     * Token validation result
     */
    public record TokenValidationResult(
            boolean valid,
            String userId,
            String userType,
            String sessionId
    ) {}
}
