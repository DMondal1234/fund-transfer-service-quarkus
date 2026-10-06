package com.lbg.fundtransfer.exception;

import com.lbg.fundtransfer.domain.ErrorCode;

/**
 * Thrown when a login attempt fails (unknown user or wrong password). Replaces
 * the Spring Security {@code BadCredentialsException} of the original, and maps
 * (via {@link ErrorCode#AUTH_INVALID_CREDENTIALS}) to HTTP 401.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidCredentialsException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
