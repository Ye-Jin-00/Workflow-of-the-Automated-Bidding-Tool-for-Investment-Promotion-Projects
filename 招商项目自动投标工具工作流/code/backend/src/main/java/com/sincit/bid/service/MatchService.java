package com.sincit.bid.service;

import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.MatchReport;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.QualificationResult;
import com.sincit.bid.domain.QualificationResult.RuleCheck;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资质匹配评估。把“企业能做什么”和“招标要什么”放到同一张评分卡上，
 * 输出可解释的分项得分、能力证据与差距清单，而不是一个黑盒分数。
 */
@Service
public class MatchService {

    public MatchReport evaluate(BidProject project) {
        QualificationProfile profile = project.getProfile() == null
                ? QualificationProfile.demoDefault() : project.getProfile();
        QualificationResult qualification = project.getQualificationResult();

        List<MatchReport.MatchDimension> dimensions = new ArrayList<MatchReport.MatchDimension>();
        List<String> matched = new ArrayList<String>();
        List<String> gaps = new ArrayList<String>();

        int qualificationScore = qualification == null ? 0
                : (int) Math.round(qualification.getScore() * 0.30);
        dimensions.add(dimension("资质匹配", qualificationScore, 30,
                qualification == null ? "尚未完成资格预审"
                        : "预审得分 " + qualification.getScore() + "，不满足 " + qualification.getHardFailures().size() + " 项"));

        int caseCount = profile.getSimilarProjectCount() == null ? 0 : profile.getSimilarProjectCount();
        int caseScore = (int) Math.min(25, Math.round(caseCount * 3.5));
        if (profile.getSimilarProjectAmount() != null
                && profile.getSimilarProjectAmount().compareTo(new BigDecimal("2000")) >= 0) {
            caseScore = Math.min(25, caseScore + 2);
        }
        dimensions.add(dimension("业绩匹配", caseScore, 25, "近三年同类项目 " + caseCount + " 个"));
        if (caseCount >= 3) {
            matched.add("近三年 " + caseCount + " 个同类招商/运营项目业绩");
        } else {
            gaps.add("同类业绩不足 3 个，建议补充联合体业绩或细分领域案例");
        }

        int teamSize = profile.getTeamSize() == null ? 0 : profile.getTeamSize();
        int teamScore = (int) Math.min(15, Math.round(teamSize / 20.0 * 15));
        dimensions.add(dimension("团队配置", teamScore, 15, "可投入团队 " + teamSize + " 人"));

        int localScore = 0;
        if (profile.isHasGovernmentClient()) {
            localScore += 5;
            matched.add("具备政府/国资项目服务经验，熟悉区域产业政策");
        } else {
            gaps.add("缺少政府/国资客户服务经验");
        }
        localScore += 4;
        matched.add("80+ 家中介合作机构、1500 名合作经纪人渠道网络");
        localScore += 3;
        localScore += 2;
        matched.add("20 所高校合作资源、100 余家商会协会资源");
        localScore += 1;
        dimensions.add(dimension("本地资源", localScore, 15, "渠道、高校、商协会资源可直接复用"));

        BigDecimal budget = project.getBudgetAmount();
        int priceScore = 0;
        if (budget != null) {
            if (budget.compareTo(new BigDecimal("300")) >= 0) {
                priceScore = 15;
            } else if (budget.compareTo(new BigDecimal("150")) >= 0) {
                priceScore = 12;
            } else if (budget.compareTo(new BigDecimal("80")) >= 0) {
                priceScore = 8;
            } else if (budget.compareTo(BigDecimal.ZERO) > 0) {
                priceScore = 5;
            }
        }
        dimensions.add(dimension("价格空间", priceScore, 15,
                budget == null ? "预算未填写" : "预算 " + budget.stripTrailingZeros().toPlainString() + " 万元，具备服务成本空间"));

        if (qualification != null) {
            for (RuleCheck check : qualification.getChecks()) {
                if (check.isPassed() && check.isMandatory()) {
                    matched.add(check.getRuleName() + "：" + check.getActualValue());
                }
            }
            gaps.addAll(qualification.getHardFailures());
            gaps.addAll(qualification.getWarnings());
        }
        if (profile.getQualifications() != null && !profile.getQualifications().isEmpty()) {
            matched.add("具备 " + profile.getQualifications().size() + " 项资质/备案，包括 "
                    + String.join("、", profile.getQualifications().subList(0, Math.min(3, profile.getQualifications().size()))));
        }

        int score = dimensions.stream().mapToInt(MatchReport.MatchDimension::getScore).sum();
        String recommendation;
        if (score >= 85) {
            recommendation = "强烈建议";
        } else if (score >= 70) {
            recommendation = "建议投标";
        } else if (score >= 60) {
            recommendation = "谨慎评估";
        } else {
            recommendation = "不建议";
        }
        String summary = String.format("综合匹配得分 %d 分，投标建议：%s。优势项 %d 条，需补强 %d 条。",
                score, recommendation, matched.size(), gaps.size());

        return MatchReport.builder()
                .score(score)
                .recommendation(recommendation)
                .dimensions(dimensions)
                .matched(matched)
                .gaps(gaps)
                .summary(summary)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private MatchReport.MatchDimension dimension(String name, int score, int fullScore, String comment) {
        return MatchReport.MatchDimension.builder()
                .name(name)
                .score(Math.max(0, Math.min(score, fullScore)))
                .fullScore(fullScore)
                .comment(comment)
                .build();
    }
}
