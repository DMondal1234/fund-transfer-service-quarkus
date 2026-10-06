package com.lbg.fundtransfer.dto;

import com.lbg.fundtransfer.domain.Beneficiary;

public record BeneficiaryResponse(
        Long id,
        String nickname,
        String accountNumber
) {
    public static BeneficiaryResponse from(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getNickname(),
                beneficiary.getAccountNumber()
        );
    }
}
