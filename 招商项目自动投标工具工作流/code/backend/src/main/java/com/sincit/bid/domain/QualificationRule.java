package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资格预审规则。业务人员可在“规则配置”中调整阈值、是否必填与权重，无需改代码。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualificationRule {

    private String code;
    private String name;
    private String category;
    private RuleField field;
    private RuleOperator operator;
    /** 阈值，含义由 field 决定；金额单位为万元 */
    private String threshold;
    /** true 时未通过直接触发自动驳回；false 时只记预警，允许“有条件通过” */
    private boolean mandatory;
    private boolean enabled;
    /** 评分权重，全部规则权重之和建议为 100 */
    private int weight;
    private String suggestion;
    private String description;

    public enum RuleField {
        REGISTERED_CAPITAL("注册资本"),
        ESTABLISHED_YEARS("成立年限"),
        CREDIT_LEVEL("信用等级"),
        SIMILAR_CASES("同类业绩"),
        QUALIFICATION_KEYWORDS("资质/备案"),
        TEAM_SIZE("团队规模"),
        BID_BOND("保证金能力"),
        BLACKLIST("失信记录"),
        CONFLICT("关联关系"),
        DEADLINE_DAYS("投标准备时间");

        private final String label;

        RuleField(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum RuleOperator {
        GTE("≥"),
        LTE("≤"),
        CONTAINS("包含"),
        LEVEL_GTE("等级不低于"),
        IS_TRUE("应为是"),
        IS_FALSE("应为否");

        private final String symbol;

        RuleOperator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}
