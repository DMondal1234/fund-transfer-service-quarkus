package com.lbg.fundtransfer.dto;

import com.lbg.fundtransfer.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String status,
        LocalDateTime timestamp,
        String description
) {
    public static TransactionResponse from(Transaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getSourceAccountNumber(),
                txn.getDestinationAccountNumber(),
                txn.getAmount(),
                txn.getStatus().name(),
                txn.getTimestamp(),
                txn.getDescription()
        );
    }
}
