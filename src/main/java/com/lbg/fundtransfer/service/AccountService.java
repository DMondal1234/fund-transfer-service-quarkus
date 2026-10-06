package com.lbg.fundtransfer.service;

import com.lbg.fundtransfer.dto.AccountResponse;

import java.util.List;

/**
 * Read access to the authenticated customer's own accounts.
 * Implemented by {@code com.lbg.fundtransfer.service.impl.AccountServiceImpl}.
 */
public interface AccountService {

    /**
     * @param username the authenticated customer
     * @return all accounts owned by that customer
     */
    List<AccountResponse> getAccountsFor(String username);

    /**
     * @param username      the authenticated customer
     * @param accountNumber the account to fetch
     * @return the account, if it belongs to the customer
     * @throws com.lbg.fundtransfer.exception.ResourceNotFoundException if missing or not owned
     */
    AccountResponse getAccount(String username, String accountNumber);
}
