package com.carddemo.partner.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyRequest {

    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;

    private List<String> scopes;

    private Integer expiresInDays; // Optional: days until expiration
}
