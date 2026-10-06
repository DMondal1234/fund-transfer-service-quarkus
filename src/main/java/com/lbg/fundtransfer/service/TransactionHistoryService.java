package com.lbg.fundtransfer.service;

import com.lbg.fundtransfer.dto.PageResponse;
import com.lbg.fundtransfer.dto.TransactionResponse;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Transaction history for the authenticated customer, filterable by an explicit
 * date range or by a calendar month. Paging is expressed as a zero-based page
 * index plus a page size (replacing Spring Data's {@code Pageable}).
 * Implemented by {@code com.lbg.fundtransfer.service.impl.TransactionHistoryServiceImpl}.
 */
public interface TransactionHistoryService {

    /**
     * @param from inclusive start date, or {@code null} for open-ended
     * @param to   inclusive end date, or {@code null} for open-ended
     * @param page zero-based page index
     * @param size page size
     */
    PageResponse<TransactionResponse> byDateRange(String username, LocalDate from, LocalDate to, int page, int size);

    /**
     * @param month the calendar month to report on
     * @param page  zero-based page index
     * @param size  page size
     */
    PageResponse<TransactionResponse> byMonth(String username, YearMonth month, int page, int size);
}
