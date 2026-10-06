package com.lbg.fundtransfer.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The single JSON error shape returned by the API. {@code code} is the stable,
 * application-specific error code (e.g. "FT-ACC-001"); {@code error} is the HTTP
 * reason phrase; {@code message} is human-readable; {@code details} lists
 * per-field validation errors when relevant.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        List<String> details
) {
    public static ErrorResponse of(int status, String error, String code, String message, List<String> details) {
        return new ErrorResponse(LocalDateTime.now(), status, error, code, message, details);
    }
}
