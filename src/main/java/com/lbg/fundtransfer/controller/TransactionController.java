package com.lbg.fundtransfer.controller;

import com.lbg.fundtransfer.domain.ErrorCode;
import com.lbg.fundtransfer.dto.PageResponse;
import com.lbg.fundtransfer.dto.TransactionResponse;
import com.lbg.fundtransfer.exception.BusinessException;
import com.lbg.fundtransfer.service.TransactionHistoryService;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Path("/api/transactions")
@Produces(MediaType.APPLICATION_JSON)
public class TransactionController {

    private final TransactionHistoryService historyService;

    public TransactionController(TransactionHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * Transaction history. Provide either {@code month=YYYY-MM} for a whole
     * month, or {@code from}/{@code to} dates (ISO {@code YYYY-MM-DD}, either
     * bound optional) for a range. With no date parameters, returns the full
     * history. Paging is controlled by {@code page} (zero-based) and {@code size}.
     */
    @GET
    public PageResponse<TransactionResponse> history(
            @Context SecurityContext securityContext,
            @QueryParam("month") String month,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        String username = securityContext.getUserPrincipal().getName();

        if (month != null && !month.isBlank()) {
            return historyService.byMonth(username, parseMonth(month), page, size);
        }
        return historyService.byDateRange(username, parseDate("from", from), parseDate("to", to), page, size);
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value); // ISO: yyyy-MM
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "month must be in format yyyy-MM");
        }
    }

    private LocalDate parseDate(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value); // ISO: yyyy-MM-dd
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    field + " must be in format yyyy-MM-dd");
        }
    }
}
