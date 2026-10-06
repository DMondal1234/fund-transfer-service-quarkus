package com.lbg.fundtransfer.dto;

import java.util.List;

/**
 * Lightweight, serialisation-friendly page wrapper so the API does not leak the
 * persistence layer's paging shape. Built directly from a content slice plus the
 * page coordinates and total element count.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
