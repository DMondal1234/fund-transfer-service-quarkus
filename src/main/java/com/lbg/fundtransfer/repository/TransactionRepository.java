package com.lbg.fundtransfer.repository;

import com.lbg.fundtransfer.domain.Transaction;
import com.lbg.fundtransfer.domain.TransactionStatus;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Panache repository for {@link Transaction}.
 */
@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {

    /** Used by merchant payments for idempotency: has this reference already succeeded? */
    public Optional<Transaction> findFirstByReferenceAndStatus(String reference, TransactionStatus status) {
        return find("reference = ?1 and status = ?2", reference, status).firstResultOptional();
    }

    /**
     * Transaction history for a set of the customer's own account numbers,
     * optionally bounded by a date range. A null bound is treated as open-ended,
     * which lets one query serve "all", "from", "until" and "between".
     *
     * <p>Returns the {@link PanacheQuery} so the caller can apply paging and read
     * the total count in one place (mirrors the Spring Data {@code Page} usage).
     */
    public PanacheQuery<Transaction> findHistory(List<String> accountNumbers,
                                                 LocalDateTime from,
                                                 LocalDateTime to) {
        String ql = """
                sourceAccountNumber in :accountNumbers
                  and (:from is null or timestamp >= :from)
                  and (:to is null or timestamp <= :to)
                """;
        Parameters params = Parameters
                .with("accountNumbers", accountNumbers)
                .and("from", from)
                .and("to", to);
        return find(ql, params);
    }
}
