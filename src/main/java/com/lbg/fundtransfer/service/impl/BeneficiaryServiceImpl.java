package com.lbg.fundtransfer.service.impl;

import com.lbg.fundtransfer.domain.Account;
import com.lbg.fundtransfer.domain.Beneficiary;
import com.lbg.fundtransfer.domain.Customer;
import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.dto.BeneficiaryRequest;
import com.lbg.fundtransfer.dto.BeneficiaryResponse;
import com.lbg.fundtransfer.exception.BusinessException;
import com.lbg.fundtransfer.exception.ResourceNotFoundException;
import com.lbg.fundtransfer.repository.AccountRepository;
import com.lbg.fundtransfer.repository.BeneficiaryRepository;
import com.lbg.fundtransfer.repository.CustomerRepository;
import com.lbg.fundtransfer.service.BeneficiaryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Full CRUD over the authenticated customer's beneficiaries. Enforces the
 * "logical link": a beneficiary must reference an account that actually exists,
 * cannot be the customer's own account, and cannot be added twice.
 */
@ApplicationScoped
@Transactional
public class BeneficiaryServiceImpl implements BeneficiaryService {
    private static final Logger log = Logger.getLogger(BeneficiaryServiceImpl.class);

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public BeneficiaryServiceImpl(BeneficiaryRepository beneficiaryRepository,
                                  CustomerRepository customerRepository,
                                  AccountRepository accountRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public List<BeneficiaryResponse> list(String username) {
        log.debugf("Listing beneficiaries for '%s'", username);
        return beneficiaryRepository.findByCustomerUsername(username).stream()
                .map(BeneficiaryResponse::from)
                .toList();
    }

    @Override
    public BeneficiaryResponse get(String username, Long id) {
        log.debugf("Fetching beneficiary '%s' for '%s'", id, username);
        return BeneficiaryResponse.from(loadOwned(username, id));
    }

    @Override
    public BeneficiaryResponse add(String username, BeneficiaryRequest request) {
        log.debugf("Adding beneficiary for '%s'", username);
        validateLinkedAccount(username, request.accountNumber());
        if (beneficiaryRepository.existsByCustomerUsernameAndAccountNumber(username, request.accountNumber())) {
            throw new BusinessException(ErrorCode.BENEFICIARY_DUPLICATE,
                    "Beneficiary with account " + request.accountNumber() + " already exists");
        }
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                        "Customer not found: " + username));
        Beneficiary beneficiary = new Beneficiary(request.nickname(), request.accountNumber(), customer);
        beneficiaryRepository.persist(beneficiary);
        log.infof("Beneficiary added for '%s': %s -> %s", username, request.nickname(), request.accountNumber());
        return BeneficiaryResponse.from(beneficiary);
    }

    @Override
    public BeneficiaryResponse update(String username, Long id, BeneficiaryRequest request) {
        log.debugf("Updating beneficiary '%s' for '%s'", id, username);
        Beneficiary beneficiary = loadOwned(username, id);
        validateLinkedAccount(username, request.accountNumber());
        boolean accountChanged = !beneficiary.getAccountNumber().equals(request.accountNumber());
        if (accountChanged
                && beneficiaryRepository.existsByCustomerUsernameAndAccountNumber(username, request.accountNumber())) {
            throw new BusinessException(ErrorCode.BENEFICIARY_DUPLICATE,
                    "Beneficiary with account " + request.accountNumber() + " already exists");
        }
        beneficiary.updateDetails(request.nickname(), request.accountNumber());
        log.infof("Beneficiary %s updated for '%s'", id, username);
        return BeneficiaryResponse.from(beneficiary);
    }

    @Override
    public void delete(String username, Long id) {
        Beneficiary beneficiary = loadOwned(username, id);
        log.infof("Beneficiary %s deleted for '%s'", id, username);
        beneficiaryRepository.delete(beneficiary);
    }

    private Beneficiary loadOwned(String username, Long id) {
        log.debugf("Loading owned beneficiary '%s' for '%s'", id, username);
        return beneficiaryRepository.findByIdAndCustomerUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BENEFICIARY_NOT_FOUND,
                        "Beneficiary " + id + " not found"));
    }

    private void validateLinkedAccount(String username, String accountNumber) {
        log.debugf("Validating linked account '%s' for '%s'", accountNumber, username);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.BENEFICIARY_ACCOUNT_NOT_FOUND,
                        "Cannot add beneficiary: account " + accountNumber + " does not exist"));

        if (account.isMerchantAccount()) {
            throw new BusinessException(ErrorCode.BENEFICIARY_MERCHANT_ACCOUNT,
                    "Cannot add beneficiary: account " + accountNumber + " is a merchant account");
        }

        if (account.getCustomer() != null && account.getCustomer().getUsername().equals(username)) {
            throw new BusinessException(ErrorCode.BENEFICIARY_OWN_ACCOUNT,
                    "You cannot add your own account as a beneficiary");
        }
    }
}
