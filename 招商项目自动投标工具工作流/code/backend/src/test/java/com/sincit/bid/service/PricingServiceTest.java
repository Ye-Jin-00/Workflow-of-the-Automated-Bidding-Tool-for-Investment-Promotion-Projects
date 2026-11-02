package com.sincit.bid.service;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.PricingPlan;
import com.sincit.bid.domain.QualificationResult;
import com.sincit.bid.web.dto.ProjectDtos;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingServiceTest {

    private final PricingService service = new PricingService();

    @Test
    void shouldKeepRecommendedPriceWithinCostFloorAndBudgetCeiling() {
        BidProject project = new BidProject();
        project.setBudgetAmount(new BigDecimal("480"));
        project.setQualificationResult(QualificationResult.builder().score(100).build());

        ProjectDtos.MatchPriceRequest request = new ProjectDtos.MatchPriceRequest();
        request.setPersonDays(90);
        request.setDailyRate(new BigDecimal("1200"));
        request.setCompetitorCount(3);

        PricingPlan plan = service.calculate(project, request);

        assertTrue(plan.getRecommendedPrice().compareTo(plan.getFloorPrice()) >= 0);
        assertTrue(plan.getRecommendedPrice().compareTo(plan.getCeilingPrice()) <= 0);
        assertTrue(plan.getGrossMarginRate() > 0);
        assertEquals(5, plan.getCostItems().size());
        assertNotNull(plan.getStrategy());
        assertEquals(90, plan.getPersonDays());
    }

    @Test
    void shouldRejectWhenBudgetMissing() {
        BidProject project = new BidProject();
        assertThrows(BusinessException.class, () -> service.calculate(project, new ProjectDtos.MatchPriceRequest()));
    }

    @Test
    void shouldFlagCostOverrunWhenBudgetTooLow() {
        BidProject project = new BidProject();
        project.setBudgetAmount(new BigDecimal("20"));
        ProjectDtos.MatchPriceRequest request = new ProjectDtos.MatchPriceRequest();
        request.setPersonDays(100);
        request.setDailyRate(new BigDecimal("2000"));

        PricingPlan plan = service.calculate(project, request);

        assertTrue(plan.getRiskNotes().size() > 0);
        assertTrue(plan.getRecommendedPrice().compareTo(plan.getTotalCost()) < 0);
        assertTrue(plan.getGrossMarginRate() < 0);
    }
}
