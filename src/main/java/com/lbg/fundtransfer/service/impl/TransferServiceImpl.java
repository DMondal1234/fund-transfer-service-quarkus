package com.lbg.fundtransfer.service.impl;

import com.lbg.fundtransfer.domain.Account;
import com.lbg.fundtransfer.domain.Beneficiary;
import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.domain.Transaction;
import com.lbg.fundtransfer.domain.TransactionStatus;
import com.lbg.fundtransfer.dto.TransferRequest;
import com.lbg.fundtransfer.dto.TransferResponse;
import com.lbg.fundtransfer.exception.BusinessException;
import com.lbg.fundtransfer.exception.InsufficientBalanceException;
import com.lbg.fundtransfer.exception.ResourceNotFoundException;
import com.lbg.fundtransfer.repository.AccountRepository;
import com.lbg.fundtransfer.repository.BeneficiaryRepository;
import com.lbg.fundtransfer.repository.TransactionRepository;
import com.lbg.fundtransfer.service.TransferService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

/**
 * Moves money from a customer's own account to one of their saved beneficiaries.
 *
 * <p>Correctness guarantees:
 * <ul>
 *   <li><b>Atomic</b> — debit, credit and the transaction record all commit or
 *       all roll back, because the whole method runs in one transaction.</li>
 *   <li><b>Safe under concurrency</b> — the source account is read with a
 *       pessimistic write lock, so two concurrent transfers from the same
 *       account serialise and cannot both spend the same balance.</li>
 *   <li><b>Scoped</b> — the source account and beneficiary must both belong to
 *       the authenticated customer.</li>
 * </ul>
 */
@ApplicationScoped
public class TransferServiceImpl implements TransferService {
    private static final Logger log = Logger.getLogger(TransferServiceImpl.class);

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransactionRepository transactionRepository;

    public TransferServiceImpl(AccountRepository accountRepository,
                               BeneficiaryRepository beneficiaryRepository,
                               TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public TransferResponse transfer(String username, TransferRequest request) {
        log.infof("Transfer requested by '%s': amount %s from %s to beneficiary %s",
                username, request.amount(), request.sourceAccountNumber(), request.beneficiaryId());

        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndCustomerUsername(request.beneficiaryId(), username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BENEFICIARY_ACCOUNT_NOT_FOUND,
                        "Beneficiary " + request.beneficiaryId() + " not found"));

        // Lock the source account for the duration of the transaction.
        Account source = accountRepository.findByAccountNumberForUpdate(request.sourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account " + request.sourceAccountNumber() + " not found"));

        log.infof("Transfer initiated from account '%s'", source.getAccountNumber());

        if (source.getCustomer() == null || !source.getCustomer().getUsername().equals(username)) {
            throw new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "Account " + request.sourceAccountNumber() + " not found for current customer");
        }
        if (source.getAccountNumber().equals(beneficiary.getAccountNumber())) {
            throw new BusinessException(ErrorCode.TRANSFER_SAME_ACCOUNT,
                    "Source and destination accounts must differ");
        }

        Account destination = accountRepository.findByAccountNumber(beneficiary.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DESTINATION_ACCOUNT_NOT_FOUND,
                        "Destination account " + beneficiary.getAccountNumber() + " no longer exists"));

        log.infof("Transfer destination account: %s", destination.getAccountNumber());

        if (!source.hasSufficientFunds(request.amount())) {
            // Record the failed attempt for the audit trail, then reject.
            transactionRepository.persist(new Transaction(
                    source.getAccountNumber(), destination.getAccountNumber(), request.amount(),
                    LocalDateTime.now(), TransactionStatus.FAILED, request.description()));
            log.warnf("Transfer rejected (insufficient funds) on account %s for amount %s",
                    source.getAccountNumber(), request.amount());
            throw new InsufficientBalanceException(ErrorCode.INSUFFICIENT_BALANCE,
                    "Insufficient balance in account " + source.getAccountNumber());
        }

        source.debit(request.amount());
        destination.credit(request.amount());

        Transaction txn = new Transaction(
                source.getAccountNumber(), destination.getAccountNumber(), request.amount(),
                LocalDateTime.now(), TransactionStatus.SUCCESS, request.description());
        transactionRepository.persist(txn);

        log.infof("Transfer: %s completed successfully: from: %s -> to: %s with amount %s",
                txn.getId(), source.getAccountNumber(), destination.getAccountNumber(), request.amount());
        return TransferResponse.of(txn, source.getBalance());
    }
}
