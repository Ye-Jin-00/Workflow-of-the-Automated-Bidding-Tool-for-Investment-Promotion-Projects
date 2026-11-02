package com.sincit.bid.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    @Builder.Default
    private List<T> items = new ArrayList<T>();
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        int totalPages = safeSize == 0 ? 0 : (all.size() + safeSize - 1) / safeSize;
        return PageResult.<T>builder()
                .items(new ArrayList<T>(all.subList(from, to)))
                .total(all.size())
                .page(safePage)
                .size(safeSize)
                .totalPages(totalPages)
                .build();
    }
}
