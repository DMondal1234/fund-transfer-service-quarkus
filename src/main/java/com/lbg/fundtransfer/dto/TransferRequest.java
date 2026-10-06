package com.lbg.fundtransfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "sourceAccountNumber is required") String sourceAccountNumber,
        @NotNull(message = "beneficiaryId is required") Long beneficiaryId,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,
        String description
) {
}
