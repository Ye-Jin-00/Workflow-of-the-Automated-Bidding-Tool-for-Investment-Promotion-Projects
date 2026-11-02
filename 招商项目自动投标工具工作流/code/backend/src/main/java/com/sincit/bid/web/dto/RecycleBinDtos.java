package com.sincit.bid.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 回收站视图。
 */
public final class RecycleBinDtos {

    private RecycleBinDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecycleItem {
        private String id;
        private String code;
        private String name;
        private String clientName;
        private String ownerName;
        private String department;
        private String statusLabel;
        private BigDecimal budgetAmount;
        private LocalDateTime deletedAt;
        private String deletedBy;
        private String deleteReason;
        private long remainingDays;
    }
}
