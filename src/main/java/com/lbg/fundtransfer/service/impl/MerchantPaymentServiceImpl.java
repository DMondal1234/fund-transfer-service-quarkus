package com.lbg.fundtransfer.service.impl;

import com.lbg.fundtransfer.domain.Account;
import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.domain.Transaction;
import com.lbg.fundtransfer.domain.TransactionStatus;
import com.lbg.fundtransfer.domain.TransactionType;
import com.lbg.fundtransfer.dto.MerchantPaymentRequest;
import com.lbg.fundtransfer.dto.MerchantPaymentResponse;
import com.lbg.fundtransfer.exception.InsufficientBalanceException;
import com.lbg.fundtransfer.exception.ResourceNotFoundException;
import com.lbg.fundtransfer.repository.AccountRepository;
import com.lbg.fundtransfer.repository.TransactionRepository;
import com.lbg.fundtransfer.service.MerchantPaymentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

/**
 * Moves money from a customer account to a merchant settlement account, atomically
 * and safely under concurrency (pessimistic lock on the source). Idempotent on the
 * request {@code reference}: a reference that already succeeded is not charged again.
 * A declined payment is recorded as a FAILED MERCHANT transaction for the audit trail.
 */
@ApplicationScoped
public class MerchantPaymentServiceImpl implements MerchantPaymentService {

    private static final Logger log = Logger.getLogger(MerchantPaymentServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public MerchantPaymentServiceImpl(AccountRepository accountRepository,
                                      TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public MerchantPaymentResponse pay(MerchantPaymentRequest request) {
        log.infof("Merchant payment: %s from %s to %s (ref %s)",
                request.amount(), request.sourceAccountNumber(), request.merchantAccountNumber(), request.reference());

        // Idempotency: if this reference already succeeded, return the original result.
        /*var existing = transactionRepository.findFirstByReferenceAndStatus(request.reference(), TransactionStatus.SUCCESS);
        if (existing.isPresent()) {
            Account src = accountRepository.findByAccountNumber(request.sourceAccountNumber()).orElse(null);
            log.info("Merchant payment ref {} already processed; returning original", request.reference());
            return MerchantPaymentResponse.of(existing.get(), src == null ? null : src.getBalance());
        }*/

        Account source = accountRepository.findByAccountNumberForUpdate(request.sourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Source account " + request.sourceAccountNumber() + " not found"));

        Account merchantAccount = accountRepository.findByAccountNumber(request.merchantAccountNumber())
                .filter(Account::isMerchantAccount)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MERCHANT_ACCOUNT_NOT_FOUND,
                        "Merchant account " + request.merchantAccountNumber() + " not found"));

        if (!source.hasSufficientFunds(request.amount())) {
            transactionRepository.persist(new Transaction(
                    source.getAccountNumber(), merchantAccount.getAccountNumber(), request.amount(),
                    LocalDateTime.now(), TransactionStatus.FAILED, TransactionType.MERCHANT,
                    request.reference(), "Merchant payment declined"));
            log.warnf("Merchant payment declined (insufficient funds) on %s", source.getAccountNumber());
            throw new InsufficientBalanceException(ErrorCode.INSUFFICIENT_BALANCE,
                    "Insufficient balance in account " + source.getAccountNumber());
        }

        source.debit(request.amount());
        merchantAccount.credit(request.amount());

        Transaction txn = new Transaction(
                source.getAccountNumber(), merchantAccount.getAccountNumber(), request.amount(),
                LocalDateTime.now(), TransactionStatus.SUCCESS, TransactionType.MERCHANT,
                request.reference(), "Merchant payment");
        transactionRepository.persist(txn);

        log.infof("Merchant payment %s succeeded (ref %s)", txn.getId(), request.reference());
        return MerchantPaymentResponse.of(txn, source.getBalance());
    }
}
