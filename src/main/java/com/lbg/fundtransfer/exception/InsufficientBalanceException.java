package com.lbg.fundtransfer.exception;

import com.lbg.fundtransfer.domain.ErrorCode;

/**
 * Thrown when a source account cannot cover a requested transfer amount.
 * Maps (via {@link ErrorCode#INSUFFICIENT_BALANCE}) to HTTP 422.
 */
public class InsufficientBalanceException extends ApiException {

    public InsufficientBalanceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InsufficientBalanceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
