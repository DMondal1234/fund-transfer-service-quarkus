package com.lbg.fundtransfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Internal (service-to-service) merchant payment request. Moves money by account
 * numbers — no beneficiary needed — from a customer account to a merchant account.
 */
public record MerchantPaymentRequest(
        @NotBlank(message = "sourceAccountNumber is required") String sourceAccountNumber,
        @NotBlank(message = "merchantAccountNumber is required") String merchantAccountNumber,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,
        @NotBlank(message = "reference is required") String reference
) {
}
