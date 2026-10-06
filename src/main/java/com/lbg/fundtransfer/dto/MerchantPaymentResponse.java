package com.lbg.fundtransfer.dto;

import com.lbg.fundtransfer.domain.Transaction;

import java.math.BigDecimal;

public record MerchantPaymentResponse(
        Long transactionId,
        String status,
        BigDecimal sourceBalanceAfter,
        String reference
) {
    public static MerchantPaymentResponse of(Transaction txn, BigDecimal sourceBalanceAfter) {
        return new MerchantPaymentResponse(
                txn.getId(), txn.getStatus().name(), sourceBalanceAfter, txn.getReference());
    }
}
