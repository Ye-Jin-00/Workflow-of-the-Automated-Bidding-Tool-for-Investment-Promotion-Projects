package com.sincit.bid.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作台 DTO。
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private long total;
        private long drafts;
        private long active;
        private long pendingReview;
        private long rejected;
        private long finished;
        private long todo;
        private BigDecimal totalBudget;
        private BigDecimal totalBid;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageItem {
        private int stage;
        private String label;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoItem {
        private String projectId;
        private String code;
        private String name;
        private String statusLabel;
        private String action;
        private String actionLabel;
        private LocalDateTime deadline;
        private long daysToDeadline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskItem {
        private String projectId;
        private String code;
        private String name;
        private String level;
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        private Stats stats;
        @Builder.Default
        private List<StageItem> stages = new ArrayList<StageItem>();
        @Builder.Default
        private List<TodoItem> todos = new ArrayList<TodoItem>();
        @Builder.Default
        private List<ProjectDtos.Summary> recentProjects = new ArrayList<ProjectDtos.Summary>();
        @Builder.Default
        private List<RiskItem> risks = new ArrayList<RiskItem>();
        private LocalDateTime generatedAt;
    }
}
