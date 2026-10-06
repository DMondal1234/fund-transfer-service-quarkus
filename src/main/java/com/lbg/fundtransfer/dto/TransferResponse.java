package com.lbg.fundtransfer.dto;

import com.lbg.fundtransfer.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long transactionId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String status,
        LocalDateTime timestamp,
        BigDecimal sourceBalanceAfter
) {
    public static TransferResponse of(Transaction txn, BigDecimal sourceBalanceAfter) {
        return new TransferResponse(
                txn.getId(),
                txn.getSourceAccountNumber(),
                txn.getDestinationAccountNumber(),
                txn.getAmount(),
                txn.getStatus().name(),
                txn.getTimestamp(),
                sourceBalanceAfter
        );
    }
}
