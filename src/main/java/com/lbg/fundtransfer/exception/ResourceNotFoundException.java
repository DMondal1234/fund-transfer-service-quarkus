package com.lbg.fundtransfer.exception;

import com.lbg.fundtransfer.domain.ErrorCode;

/**
 * Thrown when a requested resource does not exist or is not visible to the
 * current customer. The {@link ErrorCode} decides the exact code/status
 * (e.g. ACCOUNT_NOT_FOUND, BENEFICIARY_NOT_FOUND).
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
