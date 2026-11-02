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
 * 报价测算结果，金额单位统一为万元。成本项、下浮率与竞争家数均可回溯。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingPlan {

    private BigDecimal budgetAmount;
    @Builder.Default
    private List<CostItem> costItems = new ArrayList<CostItem>();
    private BigDecimal totalCost;
    private BigDecimal floorPrice;
    private BigDecimal ceilingPrice;
    private BigDecimal recommendedPrice;
    /** 毛利率，百分数，如 18.60 表示 18.60% */
    private double grossMarginRate;
    /** 价格分，满分 30 */
    private double priceScore;
    /** 中标概率预估，百分数 */
    private double winProbability;
    private String strategy;
    @Builder.Default
    private List<String> riskNotes = new ArrayList<String>();
    private int personDays;
    private BigDecimal dailyRate;
    private double subcontractRate;
    private double travelRate;
    private double taxRate;
    private double discountRate;
    private int competitorCount;
    private LocalDateTime calculatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostItem {
        private String name;
        private BigDecimal amount;
        private String remark;
    }
}
