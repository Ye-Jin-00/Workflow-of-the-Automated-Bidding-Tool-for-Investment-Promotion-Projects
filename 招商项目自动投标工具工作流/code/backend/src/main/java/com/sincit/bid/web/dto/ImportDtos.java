package com.sincit.bid.web.dto;

import com.sincit.bid.domain.RequirementItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 招标文件导入识别结果。
 */
public final class ImportDtos {

    private ImportDtos() {
    }

    @Data
    public static class ParseRequest {
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportResult {
        private String projectName;
        private String clientName;
        private String industry;
        private String source;
        private BigDecimal budgetAmount;
        private BigDecimal depositAmount;
        private LocalDateTime deadline;
        private String tenderText;
        private String summary;
        private int charCount;
        @Builder.Default
        private List<RequirementItem> requirements = new ArrayList<RequirementItem>();
        @Builder.Default
        private List<String> warnings = new ArrayList<String>();
        private String filename;
    }
}
