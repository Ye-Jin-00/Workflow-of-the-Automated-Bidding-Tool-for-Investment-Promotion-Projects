package com.sincit.bid.service;

import com.sincit.bid.web.dto.ImportDtos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportServiceTest {

    private final ImportService importService = new ImportService(new ExtractionService(), new TableFileReader());

    @Test
    void shouldRecognizeBasicFieldsFromTenderText() {
        String text = "项目名称：南京某产业园招商代理服务\n"
                + "招标人：南京某开发建设有限公司\n"
                + "预算金额：人民币300万元\n"
                + "投标保证金：人民币5万元\n"
                + "投标截止时间：2026年12月20日 17:00\n"
                + "一、资格要求\n"
                + "1. 投标人须具备房地产经纪机构备案，注册资本不低于300万元。\n"
                + "2. 近三年须具有不少于2个同类项目业绩。\n"
                + "3. 信用等级AA级及以上，无失信记录。\n"
                + "二、服务要求\n"
                + "4. 负责产业定位、招商策略、渠道组织与企业落地服务。\n"
                + "5. 合作期内完成可招商面积60%以上去化。\n"
                + "三、商务要求\n"
                + "6. 投标保证金须在截止前缴纳，付款方式30%/40%/30%。\n"
                + "四、评标办法\n"
                + "7. 综合评分法：价格分30分、技术分50分、商务分20分。\n"
                + "8. 服务期12个月，团队须驻场。";

        ImportDtos.ImportResult result = importService.parseText(text, "test.txt");

        assertEquals("南京某产业园招商代理服务", result.getProjectName());
        assertEquals("南京某开发建设有限公司", result.getClientName());
        assertEquals(0, result.getBudgetAmount().compareTo(new java.math.BigDecimal("300")));
        assertEquals(0, result.getDepositAmount().compareTo(new java.math.BigDecimal("5")));
        assertNotNull(result.getDeadline());
        assertTrue(result.getDeadline().getDayOfMonth() == 20);
        assertTrue(result.getRequirements().size() >= 8);
        assertTrue(result.getCharCount() > 100);
    }
}
