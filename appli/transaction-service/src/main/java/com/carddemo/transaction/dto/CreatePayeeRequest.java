package com.carddemo.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePayeeRequest {

    @NotNull(message = "Customer ID is required")
    private Integer customerId;

    @NotBlank(message = "Payee name is required")
    private String payeeName;

    @NotBlank(message = "Payee type is required")
    private String payeeType;

    private String payeeAccountNumber;

    private String nickname;
}
