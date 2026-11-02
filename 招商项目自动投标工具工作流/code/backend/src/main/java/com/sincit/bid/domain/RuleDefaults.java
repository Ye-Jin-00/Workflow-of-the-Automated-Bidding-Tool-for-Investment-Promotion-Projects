package com.sincit.bid.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置资格规则。规则落库后会以数据库配置为准，此处仅用于首次初始化与恢复默认。
 */
public final class RuleDefaults {

    private RuleDefaults() {
    }

    public static List<QualificationRule> all() {
        List<QualificationRule> rules = new ArrayList<QualificationRule>();
        rules.add(rule("Q01", "注册资本", "主体资格", QualificationRule.RuleField.REGISTERED_CAPITAL,
                QualificationRule.RuleOperator.GTE, "300", true, true, 15,
                "注册资本不足，建议联合体投标或调整标的",
                "招商代理类项目通常要求投标人具备一定资本实力，具体以招标文件为准。"));
        rules.add(rule("Q02", "成立年限", "主体资格", QualificationRule.RuleField.ESTABLISHED_YEARS,
                QualificationRule.RuleOperator.GTE, "3", true, true, 10,
                "成立年限不足，需以联合体或授权方式补充",
                "按当前年份减去成立年份计算。"));
        rules.add(rule("Q03", "信用等级", "信用合规", QualificationRule.RuleField.CREDIT_LEVEL,
                QualificationRule.RuleOperator.LEVEL_GTE, "AA", true, true, 10,
                "信用等级不满足，需提供信用修复或补充担保",
                "AAA > AA > A > BBB，其余视为不满足。"));
        rules.add(rule("Q04", "同类项目业绩", "业绩能力", QualificationRule.RuleField.SIMILAR_CASES,
                QualificationRule.RuleOperator.GTE, "2", true, true, 20,
                "同类业绩不足，建议补充联合体或分包业绩",
                "近三年同类招商/运营/品牌顾问项目数量。"));
        rules.add(rule("Q05", "核心资质/备案", "主体资格", QualificationRule.RuleField.QUALIFICATION_KEYWORDS,
                QualificationRule.RuleOperator.CONTAINS, "招商运营|房地产经纪|产业园区运营", true, true, 10,
                "缺少关键资质或备案，需补齐后再投标",
                "满足任一关键词即视为具备核心资质。"));
        rules.add(rule("Q06", "团队规模", "团队能力", QualificationRule.RuleField.TEAM_SIZE,
                QualificationRule.RuleOperator.GTE, "10", false, true, 5,
                "团队配置偏少，需补充驻场人员",
                "可投入本项目的专职人员数量。"));
        rules.add(rule("Q07", "保证金能力", "商务履约", QualificationRule.RuleField.BID_BOND,
                QualificationRule.RuleOperator.IS_TRUE, "true", false, true, 5,
                "保证金资金未落实，需提前调度或申请保函",
                "是否可在截止前足额缴纳投标保证金。"));
        rules.add(rule("Q08", "失信/黑名单", "信用合规", QualificationRule.RuleField.BLACKLIST,
                QualificationRule.RuleOperator.IS_FALSE, "false", true, true, 15,
                "存在失信记录，一票否决",
                "信用中国、政府采购网等渠道核查结果。"));
        rules.add(rule("Q09", "关联关系", "信用合规", QualificationRule.RuleField.CONFLICT,
                QualificationRule.RuleOperator.IS_FALSE, "false", true, true, 5,
                "存在利益冲突，需回避或取得招标人书面同意",
                "与招标人、代理机构是否存在关联关系。"));
        rules.add(rule("Q10", "投标准备时间", "时间要求", QualificationRule.RuleField.DEADLINE_DAYS,
                QualificationRule.RuleOperator.GTE, "3", true, true, 5,
                "距投标截止不足 3 天，存在制作与盖章风险",
                "按自然日计算；不足 3 天原则上不接新标。"));
        return rules;
    }

    private static QualificationRule rule(String code, String name, String category,
                                          QualificationRule.RuleField field,
                                          QualificationRule.RuleOperator operator, String threshold,
                                          boolean mandatory, boolean enabled, int weight,
                                          String suggestion, String description) {
        QualificationRule rule = new QualificationRule();
        rule.setCode(code);
        rule.setName(name);
        rule.setCategory(category);
        rule.setField(field);
        rule.setOperator(operator);
        rule.setThreshold(threshold);
        rule.setMandatory(mandatory);
        rule.setEnabled(enabled);
        rule.setWeight(weight);
        rule.setSuggestion(suggestion);
        rule.setDescription(description);
        return rule;
    }
}
