package com.lbg.fundtransfer.exception;

import com.lbg.fundtransfer.domain.ErrorCode;

/**
 * Thrown for business-rule violations that are the caller's fault but are not a
 * missing resource or an insufficient balance (e.g. duplicate beneficiary,
 * adding your own account, transferring to yourself). The {@link ErrorCode}
 * decides the exact code/status.
 */
public class BusinessException extends ApiException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
