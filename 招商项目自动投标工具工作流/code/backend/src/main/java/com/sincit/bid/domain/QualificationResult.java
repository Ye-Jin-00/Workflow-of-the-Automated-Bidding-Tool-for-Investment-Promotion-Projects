package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资格预审结果。保留每一条规则的实测值与期望值，方便投标经理快速定位短板。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationResult {

    private boolean passed;
    /** PASS / CONDITIONAL_PASS / REJECT */
    private String conclusion;
    private int score;
    private LocalDateTime checkedAt;
    @Builder.Default
    private List<RuleCheck> checks = new ArrayList<RuleCheck>();
    @Builder.Default
    private List<String> hardFailures = new ArrayList<String>();
    @Builder.Default
    private List<String> warnings = new ArrayList<String>();
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleCheck {
        private String ruleCode;
        private String ruleName;
        private String category;
        private boolean mandatory;
        private boolean passed;
        private String actualValue;
        private String expectedValue;
        private String message;
        private int weight;
        private String suggestion;
    }
}
