package com.lbg.fundtransfer.service.impl;

import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.dto.AccountResponse;
import com.lbg.fundtransfer.exception.ResourceNotFoundException;
import com.lbg.fundtransfer.repository.AccountRepository;
import com.lbg.fundtransfer.service.AccountService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Read access to the authenticated customer's own accounts.
 */
@ApplicationScoped
@Transactional
public class AccountServiceImpl implements AccountService {
    private static final Logger log = Logger.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public List<AccountResponse> getAccountsFor(String username) {
        log.debugf("Listing accounts for '%s'", username);
        return accountRepository.findByCustomerUsername(username).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Override
    public AccountResponse getAccount(String username, String accountNumber) {
        log.debugf("Fetching account '%s' for '%s'", accountNumber, username);
        return accountRepository.findByAccountNumber(accountNumber)
                .filter(account -> account.getCustomer() != null
                        && account.getCustomer().getUsername().equals(username))
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account " + accountNumber + " not found for current customer"));
    }
}
