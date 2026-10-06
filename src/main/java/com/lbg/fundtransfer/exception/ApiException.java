package com.lbg.fundtransfer.exception;

import com.lbg.fundtransfer.domain.ErrorCode;

/**
 * Base type for all business exceptions. Carries an {@link ErrorCode}, which
 * supplies both the machine-readable code and the HTTP status, so the
 * {@code GlobalExceptionHandler} can translate any subclass uniformly.
 */
public abstract class ApiException extends RuntimeException {

    private final transient ErrorCode errorCode;

    protected ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    protected ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
