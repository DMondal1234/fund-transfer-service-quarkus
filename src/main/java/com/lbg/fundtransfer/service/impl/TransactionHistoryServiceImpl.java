package com.lbg.fundtransfer.service.impl;

import com.lbg.fundtransfer.domain.Transaction;
import com.lbg.fundtransfer.dto.PageResponse;
import com.lbg.fundtransfer.dto.TransactionResponse;
import com.lbg.fundtransfer.repository.AccountRepository;
import com.lbg.fundtransfer.repository.TransactionRepository;
import com.lbg.fundtransfer.service.TransactionHistoryService;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Transaction history for the authenticated customer, filterable by an explicit
 * date range or by a calendar month.
 */
@ApplicationScoped
@Transactional
public class TransactionHistoryServiceImpl implements TransactionHistoryService {
    private static final Logger log = Logger.getLogger(TransactionHistoryServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionHistoryServiceImpl(TransactionRepository transactionRepository,
                                         AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public PageResponse<TransactionResponse> byDateRange(String username, LocalDate from, LocalDate to,
                                                         int page, int size) {
        LocalDateTime fromTs = from == null ? null : from.atStartOfDay();
        LocalDateTime toTs = to == null ? null : to.atTime(23, 59, 59);
        return query(username, fromTs, toTs, page, size);
    }

    @Override
    public PageResponse<TransactionResponse> byMonth(String username, YearMonth month, int page, int size) {
        LocalDateTime fromTs = month.atDay(1).atStartOfDay();
        LocalDateTime toTs = month.atEndOfMonth().atTime(23, 59, 59);
        return query(username, fromTs, toTs, page, size);
    }

    private PageResponse<TransactionResponse> query(String username, LocalDateTime from, LocalDateTime to,
                                                    int page, int size) {
        log.debugf("History query for '%s' [%s .. %s]", username, from, to);
        List<String> ownedAccounts = accountRepository.findByCustomerUsername(username).stream()
                .map(account -> account.getAccountNumber())
                .toList();
        if (ownedAccounts.isEmpty()) {
            return PageResponse.of(List.of(), page, size, 0);
        }
        PanacheQuery<Transaction> pq = transactionRepository.findHistory(ownedAccounts, from, to);
        long total = pq.count();
        List<TransactionResponse> content = pq.page(Page.of(page, size)).list().stream()
                .map(TransactionResponse::from)
                .toList();
        return PageResponse.of(content, page, size, total);
    }
}
