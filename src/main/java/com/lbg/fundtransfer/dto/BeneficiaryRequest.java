package com.lbg.fundtransfer.dto;

import jakarta.validation.constraints.NotBlank;

public record BeneficiaryRequest(
        @NotBlank(message = "nickname is required") String nickname,
        @NotBlank(message = "accountNumber is required") String accountNumber
) {
}
