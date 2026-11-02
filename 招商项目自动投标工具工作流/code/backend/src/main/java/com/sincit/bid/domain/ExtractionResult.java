package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 招标文件要素抽取结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {

    @Builder.Default
    private List<RequirementItem> items = new ArrayList<RequirementItem>();
    private LocalDateTime extractedAt;
    /** RULE / AI / MANUAL */
    private String source;
    private int charCount;
    private String summary;
    /** 抽取到的结构化字段，可由用户确认后回填项目 */
    private BigDecimal budgetAmount;
    private BigDecimal depositAmount;
    private LocalDateTime deadline;
    @Builder.Default
    private List<String> warnings = new ArrayList<String>();
}
