package com.sincit.bid.service;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.PricingPlan;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 报价测算。
 * 成本 = 人力 + 分包 + 差旅 + 其他 + 税费；在成本底线与最高限价之间给出建议价、毛利率、价格分与中标概率。
 * 所有假设参数随结果保存，复核人可复算，避免“拍脑袋报价”。
 */
@Service
public class PricingService {

    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");

    public PricingPlan calculate(BidProject project, ProjectDtos.MatchPriceRequest request) {
        BigDecimal budget = project.getBudgetAmount();
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.invalid("请先在项目信息中填写招标预算/最高限价，再进行报价测算");
        }
        ProjectDtos.MatchPriceRequest req = request == null ? new ProjectDtos.MatchPriceRequest() : request;
        int personDays = positive(req.getPersonDays(), 60);
        BigDecimal dailyRate = req.getDailyRate() == null ? new BigDecimal("1200") : req.getDailyRate();
        double subcontractRate = rate(req.getSubcontractRate(), 0.05);
        double travelRate = rate(req.getTravelRate(), 0.02);
        double taxRate = rate(req.getTaxRate(), 0.06);
        double discountRate = rate(req.getDiscountRate(), 0.08);
        int competitors = positive(req.getCompetitorCount(), 4);

        if (dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.invalid("人天单价必须大于 0");
        }

        BigDecimal personnel = dailyRate.multiply(new BigDecimal(personDays))
                .divide(TEN_THOUSAND, 2, RoundingMode.HALF_UP);
        BigDecimal subcontract = budget.multiply(BigDecimal.valueOf(subcontractRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal travel = budget.multiply(BigDecimal.valueOf(travelRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal other = budget.multiply(new BigDecimal("0.01")).max(new BigDecimal("2")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = personnel.add(subcontract).add(travel).add(other);
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(taxRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = subtotal.add(tax);

        BigDecimal floor = totalCost.multiply(new BigDecimal("1.12")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ceiling = budget.multiply(new BigDecimal("0.96")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal target = budget.multiply(BigDecimal.valueOf(1 - discountRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal recommended = target.max(floor).min(ceiling);
        List<String> risks = new ArrayList<String>();
        if (floor.compareTo(ceiling) > 0) {
            recommended = ceiling;
            risks.add("成本底线高于可报价上限，存在成本倒挂风险，建议缩减投入或放弃投标");
        }
        if (recommended.compareTo(budget) > 0) {
            recommended = ceiling;
            risks.add("建议报价超过最高限价，已自动压回限价以内");
        }

        double margin = recommended.compareTo(BigDecimal.ZERO) == 0 ? 0
                : recommended.subtract(totalCost)
                .divide(recommended, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).doubleValue();

        // 价格分模拟：以预算的 88% 作为模拟基准价，偏离越小得分越高（满分 30）。
        BigDecimal benchmark = budget.multiply(new BigDecimal("0.88")).setScale(2, RoundingMode.HALF_UP);
        double deviation = benchmark.compareTo(BigDecimal.ZERO) == 0 ? 0
                : recommended.subtract(benchmark).abs()
                .divide(benchmark, 4, RoundingMode.HALF_UP).doubleValue();
        double priceScore = clamp(30 - deviation * 100 * 0.6, 0, 30);
        double marginScore = clamp(margin * 4, 0, 100);
        double competitionScore = clamp(100 - competitors * 8, 20, 100);
        double qualificationScore = project.getQualificationResult() == null ? 60
                : project.getQualificationResult().getScore();
        double winProbability = qualificationScore * 0.35 + (priceScore / 30 * 100) * 0.30
                + marginScore * 0.15 + competitionScore * 0.20;
        winProbability = clamp(winProbability, 0, 99);

        double priceRatio = recommended.divide(budget, 4, RoundingMode.HALF_UP).doubleValue();
        String strategy;
        if (margin < 5) {
            strategy = "保守报价（成本承压）";
            risks.add("毛利率低于 5%，抗风险能力弱，需严格锁定交付成本");
        } else if (priceRatio <= 0.86) {
            strategy = "低价抢标";
        } else if (priceRatio <= 0.93) {
            strategy = "均衡报价";
        } else {
            strategy = "价值报价";
            risks.add("报价接近最高限价，价格分不占优，需以技术分和业绩分弥补");
        }
        if (competitors >= 6) {
            risks.add("预计竞争家数较多（" + competitors + " 家），建议强化差异化方案与服务承诺");
        }
        if (margin >= 5 && margin < 10) {
            risks.add("毛利率处于 5%-10% 区间，建议在合同中明确增项与付款节点");
        }

        List<PricingPlan.CostItem> costItems = new ArrayList<PricingPlan.CostItem>();
        costItems.add(item("人力成本", personnel, personDays + " 人天 × " + dailyRate.stripTrailingZeros().toPlainString() + " 元/人天"));
        costItems.add(item("分包/渠道成本", subcontract, "预算的 " + percent(subcontractRate)));
        costItems.add(item("差旅与尽调", travel, "预算的 " + percent(travelRate)));
        costItems.add(item("其他直接费用", other, "按预算 1% 且不低于 2 万元"));
        costItems.add(item("税费", tax, "成本的 " + percent(taxRate)));

        return PricingPlan.builder()
                .budgetAmount(budget)
                .costItems(costItems)
                .totalCost(totalCost)
                .floorPrice(floor)
                .ceilingPrice(ceiling)
                .recommendedPrice(recommended)
                .grossMarginRate(round2(margin))
                .priceScore(round2(priceScore))
                .winProbability(round2(winProbability))
                .strategy(strategy)
                .riskNotes(risks)
                .personDays(personDays)
                .dailyRate(dailyRate)
                .subcontractRate(subcontractRate)
                .travelRate(travelRate)
                .taxRate(taxRate)
                .discountRate(discountRate)
                .competitorCount(competitors)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    private PricingPlan.CostItem item(String name, BigDecimal amount, String remark) {
        return PricingPlan.CostItem.builder().name(name).amount(amount).remark(remark).build();
    }

    private int positive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private double rate(Double value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(0, Math.min(value, 0.5));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String percent(double value) {
        return BigDecimal.valueOf(value * 100).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
