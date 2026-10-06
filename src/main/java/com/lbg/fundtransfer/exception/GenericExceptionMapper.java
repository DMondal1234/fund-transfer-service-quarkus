package com.lbg.fundtransfer.exception;

import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.dto.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Catch-all so unexpected failures still return the standard error shape, not a
 * raw stack trace — the equivalent of the Spring {@code @ExceptionHandler(Exception.class)}.
 *
 * <p>JAX-RS {@link WebApplicationException}s (e.g. a 404 for an unknown route, a
 * 405 for a wrong method) carry their own response and are passed through
 * unchanged rather than being rewritten to 500.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger log = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception ex) {
        if (ex instanceof WebApplicationException wae) {
            return wae.getResponse();
        }
        log.error("Unexpected error occurred", ex);
        ErrorResponse body = ErrorResponse.of(
                ErrorCode.INTERNAL_ERROR.getStatusCode(),
                ErrorCode.INTERNAL_ERROR.getReasonPhrase(),
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                List.of());
        return Response.status(ErrorCode.INTERNAL_ERROR.getStatusCode()).entity(body).build();
    }
}
