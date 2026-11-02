package com.sincit.bid.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作与批量导入结果。
 */
public final class BatchDtos {

    private BatchDtos() {
    }

    @Data
    public static class IdsRequest {
        private List<String> ids = new ArrayList<String>();
    }

    @Data
    public static class DeleteRequest {
        private List<String> ids = new ArrayList<String>();
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchResult {
        private int success;
        private int failed;
        @Builder.Default
        private List<Failure> failures = new ArrayList<Failure>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Failure {
        private String id;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchImportResult {
        private int total;
        private int success;
        private int failed;
        @Builder.Default
        private List<ImportSuccess> items = new ArrayList<ImportSuccess>();
        @Builder.Default
        private List<ImportFailure> errors = new ArrayList<ImportFailure>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportSuccess {
        private int row;
        private String id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportFailure {
        private int row;
        private String message;
    }
}
