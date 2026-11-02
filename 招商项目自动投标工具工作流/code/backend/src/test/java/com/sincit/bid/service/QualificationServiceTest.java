package com.sincit.bid.service;

import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.QualificationResult;
import com.sincit.bid.domain.RuleDefaults;
import com.sincit.bid.persistence.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QualificationServiceTest {

    private QualificationService service;

    @BeforeEach
    void setUp() {
        RuleRepository ruleRepository = mock(RuleRepository.class);
        when(ruleRepository.findAll()).thenReturn(RuleDefaults.all());
        service = new QualificationService(ruleRepository);
    }

    @Test
    void shouldPassWhenAllMandatoryRulesSatisfied() {
        BidProject project = project(LocalDateTime.now().plusDays(10));
        QualificationResult result = service.evaluate(project, QualificationProfile.demoDefault());

        assertTrue(result.isPassed());
        assertEquals("PASS", result.getConclusion());
        assertEquals(100, result.getScore());
        assertTrue(result.getHardFailures().isEmpty());
    }

    @Test
    void shouldRejectWhenMandatoryRulesFail() {
        BidProject project = project(LocalDateTime.now().plusDays(10));
        QualificationProfile weak = QualificationProfile.demoDefault();
        weak.setRegisteredCapital(new BigDecimal("50"));
        weak.setEstablishedYear(LocalDateTime.now().getYear() - 1);
        weak.setCreditLevel("A");
        weak.setSimilarProjectCount(0);
        weak.setQualifications(Arrays.asList("品牌策划服务"));

        QualificationResult result = service.evaluate(project, weak);

        assertFalse(result.isPassed());
        assertEquals("REJECT", result.getConclusion());
        assertEquals(5, result.getHardFailures().size());
        assertTrue(result.getScore() < 100);
    }

    @Test
    void shouldAllowConditionalPassWhenOnlySoftRulesFail() {
        BidProject project = project(LocalDateTime.now().plusDays(10));
        QualificationProfile profile = QualificationProfile.demoDefault();
        profile.setTeamSize(3);
        profile.setBidBondAvailable(false);

        QualificationResult result = service.evaluate(project, profile);

        assertTrue(result.isPassed());
        assertEquals("CONDITIONAL_PASS", result.getConclusion());
        assertEquals(2, result.getWarnings().size());
    }

    @Test
    void shouldRejectWhenDeadlineTooClose() {
        BidProject project = project(LocalDateTime.now().plusDays(1));
        QualificationResult result = service.evaluate(project, QualificationProfile.demoDefault());

        assertFalse(result.isPassed());
        assertEquals(1, result.getHardFailures().size());
    }

    private BidProject project(LocalDateTime deadline) {
        BidProject project = new BidProject();
        project.setName("测试项目");
        project.setDeadline(deadline);
        return project;
    }
}
