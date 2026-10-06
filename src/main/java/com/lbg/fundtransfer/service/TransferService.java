package com.lbg.fundtransfer.service;

import com.lbg.fundtransfer.dto.TransferRequest;
import com.lbg.fundtransfer.dto.TransferResponse;

/**
 * Moves money from a customer's own account to one of their saved beneficiaries,
 * atomically and safely under concurrency.
 * Implemented by {@code com.lbg.fundtransfer.service.impl.TransferServiceImpl}.
 */
public interface TransferService {

    /**
     * Performs the transfer as a single transaction (debit + credit + ledger
     * record commit or roll back together).
     *
     * @param username the authenticated customer (must own the source account)
     * @param request  source account, beneficiary, amount and optional description
     * @return the resulting transaction and the source balance after the debit
     * @throws com.lbg.fundtransfer.exception.ResourceNotFoundException if the beneficiary or
     *         source account is missing or not owned by the customer
     * @throws com.lbg.fundtransfer.exception.BusinessException if source and destination are the same
     * @throws com.lbg.fundtransfer.exception.InsufficientBalanceException if the balance is too low
     */
    TransferResponse transfer(String username, TransferRequest request);
}
