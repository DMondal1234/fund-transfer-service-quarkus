package com.lbg.fundtransfer.exception;

import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Maps Bean Validation ({@code @Valid}) failures to the standard error body with
 * one detail line per invalid field — the Quarkus/JAX-RS equivalent of the
 * Spring {@code MethodArgumentNotValidException} handler. Registered explicitly so
 * it overrides Quarkus' built-in validation mapper and keeps the API's error shape.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    private static final Logger log = Logger.getLogger(ValidationExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException ex) {
        log.error("Validation failed", ex);
        List<String> details = ex.getConstraintViolations().stream()
                .map(this::formatViolation)
                .toList();
        ErrorResponse body = ErrorResponse.of(
                ErrorCode.VALIDATION_FAILED.getStatusCode(),
                ErrorCode.VALIDATION_FAILED.getReasonPhrase(),
                ErrorCode.VALIDATION_FAILED.getCode(),
                ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                details);
        return Response.status(ErrorCode.VALIDATION_FAILED.getStatusCode()).entity(body).build();
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        String field = null;
        for (Path.Node node : violation.getPropertyPath()) {
            field = node.getName();
        }
        return (field == null ? "request" : field) + ": " + violation.getMessage();
    }
}
