package com.lbg.fundtransfer.exception;

import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Maps any of our business exceptions ({@link ApiException} and subclasses) to the
 * consistent JSON error body, taking the code and HTTP status straight from the
 * attached {@link ErrorCode}. Replaces the {@code @ExceptionHandler(ApiException.class)}
 * method of the Spring Boot {@code GlobalExceptionHandler}.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {
    private static final Logger log = Logger.getLogger(ApiExceptionMapper.class);

    @Override
    public Response toResponse(ApiException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.errorf("API exception occurred: %s -> %s", errorCode.getCode(), ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                errorCode.getStatusCode(),
                errorCode.getReasonPhrase(),
                errorCode.getCode(),
                ex.getMessage(),
                List.of());
        return Response.status(errorCode.getStatusCode()).entity(body).build();
    }
}
