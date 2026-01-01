package com.carddemo.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Card Status Request DTO
 *
 * Used for blocking/activating cards (replaces COCRDUPC).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^[YNS]$", message = "Status must be Y (Active), N (Closed), or S (Blocked)")
    private String status;

    private String reason;
}
