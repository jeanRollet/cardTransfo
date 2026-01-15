package com.carddemo.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclineAuthorizationRequest {

    @NotBlank(message = "Decline reason is required")
    private String reason;
}
