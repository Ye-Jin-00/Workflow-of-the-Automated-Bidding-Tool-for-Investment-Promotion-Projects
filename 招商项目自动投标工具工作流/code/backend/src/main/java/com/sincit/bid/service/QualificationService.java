package com.sincit.bid.service;

import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.QualificationResult;
import com.sincit.bid.domain.QualificationResult.RuleCheck;
import com.sincit.bid.domain.QualificationRule;
import com.sincit.bid.persistence.RuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 资格预审规则引擎。
 * 规则来自配置表，按 field 分派到具体校验逻辑；必填规则失败 -> 自动驳回，非必填失败 -> 预警。
 */
@Service
public class QualificationService {

    private final RuleRepository ruleRepository;

    public QualificationService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public QualificationResult evaluate(BidProject project, QualificationProfile profile) {
        QualificationProfile actualProfile = profile == null ? QualificationProfile.demoDefault() : profile;
        List<QualificationRule> rules = ruleRepository.findAll();
        List<RuleCheck> checks = new ArrayList<RuleCheck>();
        List<String> hardFailures = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();
        int totalWeight = 0;
        int passedWeight = 0;

        for (QualificationRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            RuleCheck check = check(rule, project, actualProfile);
            checks.add(check);
            totalWeight += Math.max(rule.getWeight(), 0);
            if (check.isPassed()) {
                passedWeight += Math.max(rule.getWeight(), 0);
            } else if (rule.isMandatory()) {
                hardFailures.add(rule.getName() + "：" + check.getMessage());
            } else {
                warnings.add(rule.getName() + "：" + check.getMessage());
            }
        }

        int score = totalWeight == 0 ? 0 : (int) Math.round(passedWeight * 100.0 / totalWeight);
        boolean passed = hardFailures.isEmpty();
        String conclusion;
        if (!passed) {
            conclusion = "REJECT";
        } else if (!warnings.isEmpty()) {
            conclusion = "CONDITIONAL_PASS";
        } else {
            conclusion = "PASS";
        }
        String summary;
        if (!passed) {
            summary = String.format("资格预审未通过：%d 项必填条件不满足，系统已自动驳回。", hardFailures.size());
        } else if (!warnings.isEmpty()) {
            summary = String.format("资格预审通过（%d 项非必填条件预警，建议在投标文件中补充说明）。", warnings.size());
        } else {
            summary = "资格预审通过，全部规则满足。";
        }

        return QualificationResult.builder()
                .passed(passed)
                .conclusion(conclusion)
                .score(score)
                .checkedAt(LocalDateTime.now())
                .checks(checks)
                .hardFailures(hardFailures)
                .warnings(warnings)
                .summary(summary)
                .build();
    }

    private RuleCheck check(QualificationRule rule, BidProject project, QualificationProfile profile) {
        try {
            switch (rule.getField()) {
                case REGISTERED_CAPITAL:
                    return capitalRule(rule, profile);
                case ESTABLISHED_YEARS:
                    return establishedRule(rule, profile);
                case CREDIT_LEVEL:
                    return creditRule(rule, profile);
                case SIMILAR_CASES:
                    return similarCaseRule(rule, profile);
                case QUALIFICATION_KEYWORDS:
                    return qualificationKeywordRule(rule, profile);
                case TEAM_SIZE:
                    return teamSizeRule(rule, profile);
                case BID_BOND:
                    return bidBondRule(rule, profile);
                case BLACKLIST:
                    return blacklistRule(rule, profile);
                case CONFLICT:
                    return conflictRule(rule, profile);
                case DEADLINE_DAYS:
                    return deadlineRule(rule, project);
                default:
                    return fail(rule, "未知规则类型", "请检查规则配置");
            }
        } catch (Exception e) {
            return fail(rule, "规则执行异常", "规则配置有误：" + e.getMessage());
        }
    }

    private RuleCheck capitalRule(QualificationRule rule, QualificationProfile profile) {
        BigDecimal actual = profile.getRegisteredCapital();
        BigDecimal threshold = new BigDecimal(rule.getThreshold());
        boolean pass = actual != null && actual.compareTo(threshold) >= 0;
        String actualText = actual == null ? "未填写" : actual.stripTrailingZeros().toPlainString() + " 万元";
        return build(rule, pass, actualText, "≥ " + threshold.stripTrailingZeros().toPlainString() + " 万元",
                pass ? "注册资本满足要求" : "注册资本不足，建议联合体投标或调整标的");
    }

    private RuleCheck establishedRule(QualificationRule rule, QualificationProfile profile) {
        int threshold = Integer.parseInt(rule.getThreshold());
        Integer establishedYear = profile.getEstablishedYear();
        int years = establishedYear == null ? 0 : Math.max(0, Year.now().getValue() - establishedYear);
        boolean pass = establishedYear != null && years >= threshold;
        return build(rule, pass, years + " 年", "≥ " + threshold + " 年",
                pass ? "成立年限满足要求" : "成立年限不足，需以联合体或授权方式补充");
    }

    private RuleCheck creditRule(QualificationRule rule, QualificationProfile profile) {
        int actualRank = creditRank(profile.getCreditLevel());
        int expectedRank = creditRank(rule.getThreshold());
        boolean pass = actualRank >= 0 && actualRank >= expectedRank;
        return build(rule, pass, profile.getCreditLevel() == null ? "未填写" : profile.getCreditLevel(),
                "不低于 " + rule.getThreshold(),
                pass ? "信用等级满足要求" : "信用等级不满足，需提供信用修复或补充担保");
    }

    private RuleCheck similarCaseRule(QualificationRule rule, QualificationProfile profile) {
        int threshold = Integer.parseInt(rule.getThreshold());
        int actual = profile.getSimilarProjectCount() == null ? 0 : profile.getSimilarProjectCount();
        boolean pass = actual >= threshold;
        String amount = profile.getSimilarProjectAmount() == null ? "-"
                : profile.getSimilarProjectAmount().stripTrailingZeros().toPlainString() + " 万元";
        return build(rule, pass, actual + " 个（合同额 " + amount + "）", "≥ " + threshold + " 个",
                pass ? "同类业绩数量满足要求" : "同类业绩不足，建议补充联合体或分包业绩");
    }

    private RuleCheck qualificationKeywordRule(QualificationRule rule, QualificationProfile profile) {
        String[] keywords = rule.getThreshold().split("[|,，、]");
        List<String> owned = profile.getQualifications() == null ? new ArrayList<String>() : profile.getQualifications();
        List<String> hits = new ArrayList<String>();
        for (String keyword : keywords) {
            String k = keyword.trim();
            if (k.isEmpty()) {
                continue;
            }
            for (String item : owned) {
                if (item != null && item.contains(k)) {
                    hits.add(k);
                    break;
                }
            }
        }
        boolean pass = !hits.isEmpty();
        return build(rule, pass, owned.isEmpty() ? "未填写" : String.join("、", hits) + "（共 " + owned.size() + " 项）",
                "具备 " + rule.getThreshold(),
                pass ? "资质/备案满足要求" : "缺少关键资质或备案，需补齐后再投标");
    }

    private RuleCheck teamSizeRule(QualificationRule rule, QualificationProfile profile) {
        int threshold = Integer.parseInt(rule.getThreshold());
        int actual = profile.getTeamSize() == null ? 0 : profile.getTeamSize();
        boolean pass = actual >= threshold;
        return build(rule, pass, actual + " 人", "≥ " + threshold + " 人",
                pass ? "团队规模满足要求" : "团队配置偏少，需补充驻场人员");
    }

    private RuleCheck bidBondRule(QualificationRule rule, QualificationProfile profile) {
        boolean pass = profile.isBidBondAvailable();
        return build(rule, pass, pass ? "可覆盖" : "不足", "可覆盖投标保证金",
                pass ? "保证金资金安排已落实" : "保证金资金未落实，需提前调度或申请保函");
    }

    private RuleCheck blacklistRule(QualificationRule rule, QualificationProfile profile) {
        boolean pass = !profile.isBlacklisted();
        return build(rule, pass, profile.isBlacklisted() ? "存在失信记录" : "无失信记录", "无失信/黑名单记录",
                pass ? "信用记录正常" : "存在失信记录，一票否决");
    }

    private RuleCheck conflictRule(QualificationRule rule, QualificationProfile profile) {
        boolean pass = !profile.isConflictsOfInterest();
        return build(rule, pass, profile.isConflictsOfInterest() ? "存在关联关系" : "无关联关系", "无利益冲突",
                pass ? "关联关系声明正常" : "存在利益冲突，需回避或取得招标人书面同意");
    }

    private RuleCheck deadlineRule(QualificationRule rule, BidProject project) {
        int threshold = Integer.parseInt(rule.getThreshold());
        LocalDateTime deadline = project.getDeadline();
        long days = deadline == null ? -1 : ChronoUnit.DAYS.between(LocalDateTime.now(), deadline);
        boolean pass = deadline != null && days >= threshold;
        String actual = deadline == null ? "未设置截止时间" : days + " 天";
        return build(rule, pass, actual, "≥ " + threshold + " 天准备时间",
                pass ? "投标准备时间充足" : "距投标截止不足 " + threshold + " 天，存在制作与盖章风险");
    }

    private RuleCheck build(QualificationRule rule, boolean pass, String actual, String expected, String suggestion) {
        return RuleCheck.builder()
                .ruleCode(rule.getCode())
                .ruleName(rule.getName())
                .category(rule.getCategory())
                .mandatory(rule.isMandatory())
                .passed(pass)
                .actualValue(actual)
                .expectedValue(expected)
                .message(pass ? "通过：" + suggestion : suggestion)
                .weight(rule.getWeight())
                .suggestion(suggestion)
                .build();
    }

    private RuleCheck fail(QualificationRule rule, String actual, String message) {
        return RuleCheck.builder()
                .ruleCode(rule.getCode())
                .ruleName(rule.getName())
                .category(rule.getCategory())
                .mandatory(rule.isMandatory())
                .passed(false)
                .actualValue(actual)
                .expectedValue(rule.getThreshold())
                .message(message)
                .weight(rule.getWeight())
                .suggestion(message)
                .build();
    }

    private int creditRank(String level) {
        if (level == null) {
            return -1;
        }
        switch (level.trim().toUpperCase(Locale.ROOT)) {
            case "AAA":
                return 3;
            case "AA":
                return 2;
            case "A":
                return 1;
            case "BBB":
                return 0;
            default:
                return -1;
        }
    }
}
