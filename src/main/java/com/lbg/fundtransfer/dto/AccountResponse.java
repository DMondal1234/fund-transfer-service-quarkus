package com.lbg.fundtransfer.dto;

import com.lbg.fundtransfer.domain.Account;

import java.math.BigDecimal;

public record AccountResponse(
        String accountNumber,
        String type,
        BigDecimal balance,
        String ownerName
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getType().name(),
                account.getBalance(),
                account.getCustomer().getFullName()
        );
    }
}
