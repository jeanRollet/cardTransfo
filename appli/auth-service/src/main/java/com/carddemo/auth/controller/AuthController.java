package com.carddemo.auth.controller;

import com.carddemo.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 *
 * Replaces CICS CC00 transaction (COSGN00C COBOL program)
 *
 * CICS Flow:
 * 1. User enters CC00 → Display login screen (SEND MAP)
 * 2. User enters credentials → RECEIVE MAP
 * 3. READ USRSEC FILE with user-id
 * 4. Compare password (COBOL string comparison)
 * 5. If OK: XCTL to COMEN01C with COMMAREA
 * 6. If KO: Display error message
 *
 * Cloud Native Flow:
 * 1. POST /api/v1/auth/login with credentials
 * 2. Query PostgreSQL users table
 * 3. Verify BCrypt password hash
 * 4. Generate JWT token
 * 5. Return token + user info
 * 6. Client stores token for subsequent requests
 *
 * @author CardDemo Transformation Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and session management (replaces CC00 transaction)")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint
     *
     * CICS Equivalent: CC00 transaction (COSGN00C program)
     * VSAM File: USRSEC
     * COBOL Logic:
     *   EXEC CICS READ FILE('USRSEC')
     *                   INTO(SEC-USER-DATA)
     *                   RIDFLD(WS-USER-ID)
     *                   RESP(WS-CICS-RESP)
     *   END-EXEC.
     *
     *   IF WS-CICS-RESP = DFHRESP(NORMAL)
     *      IF SEC-USR-PWD = WS-PASSWORD
     *         MOVE SEC-USR-ID TO CA-USER-ID
     *         EXEC CICS XCTL PROGRAM('COMEN01C')
     *                         COMMAREA(WORK-COMMAREA)
     *         END-EXEC
     *      ELSE
     *         MOVE 'Invalid password' TO WS-ERROR-MSG
     *      END-IF
     *   ELSE
     *      MOVE 'User not found' TO WS-ERROR-MSG
     *   END-IF.
     *
     * @param request Login credentials (userId + password)
     * @return JWT token and user information
     */
    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user and return JWT token (replaces COSGN00C COBOL login logic)"
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUserId());

        AuthService.AuthResult result = authService.authenticate(
                request.getUserId(),
                request.getPassword()
        );

        LoginResponse response = new LoginResponse(
                result.accessToken(),
                result.refreshToken(),
                new UserInfo(
                        result.user().getUserId(),
                        result.user().getFirstName(),
                        result.user().getLastName(),
                        result.user().getUserType(),
                        result.user().getCustomerId(),
                        result.sessionId()
                )
        );

        log.info("Login successful for user: {}", request.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint
     *
     * @param token JWT token from Authorization header
     * @return Success message
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidate JWT token and clear session")
    public ResponseEntity<MessageResponse> logout(
            @Parameter(hidden = true) @RequestHeader("Authorization") String token) {
        log.info("Logout request received");

        authService.logout(token);

        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    /**
     * Token validation endpoint
     *
     * @param token JWT token from Authorization header
     * @return Token validation result
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Check if JWT token is valid and not expired")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @Parameter(hidden = true) @RequestHeader("Authorization") String token) {
        log.debug("Token validation request received");

        AuthService.TokenValidationResult result = authService.validateToken(token);

        return ResponseEntity.ok(new TokenValidationResponse(
                result.valid(),
                result.valid() ? "Token is valid" : "Token is invalid or expired"
        ));
    }

    /**
     * Refresh token endpoint
     *
     * @param request Refresh token request
     * @return New access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Generate new access token using refresh token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        AuthService.RefreshResult result = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity.ok(new RefreshTokenResponse(result.accessToken()));
    }
}

// ============================================================================
// DTOs (Data Transfer Objects)
// ============================================================================

/**
 * Login Request DTO
 * Maps to COBOL COMMAREA fields:
 * - CA-USER-ID (8 chars)
 * - CA-PASSWORD (8 chars)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class LoginRequest {
    @NotBlank(message = "User ID is required")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    private String userId;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    private String password;
}

/**
 * Login Response DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo user;
}

/**
 * User Info DTO
 * Maps to COBOL SEC-USER-DATA structure from USRSEC file
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class UserInfo {
    private String userId;        // SEC-USR-ID
    private String firstName;     // SEC-USR-FNAME
    private String lastName;      // SEC-USR-LNAME
    private String userType;      // SEC-USR-TYPE (A=Admin, U=User)
    private Integer customerId;   // Linked customer ID (null for admins)
    private String sessionId;
}

/**
 * Generic message response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class MessageResponse {
    private String message;
}

/**
 * Token validation response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class TokenValidationResponse {
    private boolean valid;
    private String message;
}

/**
 * Refresh token request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

/**
 * Refresh token response
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class RefreshTokenResponse {
    private String accessToken;
}
