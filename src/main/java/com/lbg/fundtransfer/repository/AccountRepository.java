package com.lbg.fundtransfer.repository;

import com.lbg.fundtransfer.domain.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

/**
 * Panache repository for {@link Account}. The derived-query methods of the
 * original Spring Data interface are expressed here as explicit Panache queries.
 */
@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account> {

    public Optional<Account> findByAccountNumber(String accountNumber) {
        return find("accountNumber", accountNumber).firstResultOptional();
    }

    public List<Account> findByCustomerUsername(String username) {
        return list("customer.username", username);
    }

    /**
     * Pessimistic write lock on the source account for a transfer, so two
     * concurrent debits on the same account serialise instead of racing.
     */
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        return find("accountNumber", accountNumber)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }
}
