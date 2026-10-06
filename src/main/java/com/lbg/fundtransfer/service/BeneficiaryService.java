package com.lbg.fundtransfer.service;

import com.lbg.fundtransfer.dto.BeneficiaryRequest;
import com.lbg.fundtransfer.dto.BeneficiaryResponse;

import java.util.List;

/**
 * Full CRUD over the authenticated customer's beneficiaries, enforcing the
 * "logical link" (a beneficiary must reference a real account, not the
 * customer's own, and cannot be added twice).
 * Implemented by {@code com.lbg.fundtransfer.service.impl.BeneficiaryServiceImpl}.
 */
public interface BeneficiaryService {

    /** @return all beneficiaries owned by the customer. */
    List<BeneficiaryResponse> list(String username);

    /**
     * @throws com.lbg.fundtransfer.exception.ResourceNotFoundException if missing or not owned
     */
    BeneficiaryResponse get(String username, Long id);

    /**
     * @throws com.lbg.fundtransfer.exception.BusinessException if the account does not exist,
     *         is the customer's own, or is already a beneficiary
     */
    BeneficiaryResponse add(String username, BeneficiaryRequest request);

    /**
     * @throws com.lbg.fundtransfer.exception.ResourceNotFoundException if missing or not owned
     * @throws com.lbg.fundtransfer.exception.BusinessException if the new account is invalid or a duplicate
     */
    BeneficiaryResponse update(String username, Long id, BeneficiaryRequest request);

    /**
     * @throws com.lbg.fundtransfer.exception.ResourceNotFoundException if missing or not owned
     */
    void delete(String username, Long id);
}
