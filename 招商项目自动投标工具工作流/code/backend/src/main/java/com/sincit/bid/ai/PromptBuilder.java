package com.sincit.bid.ai;

import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.RequirementItem;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 提示词构建器。集中管理提示词版本与结构，便于 A/B 测试与效果回溯。
 */
public final class PromptBuilder {

    public static final String PROMPT_VERSION = "bid-draft-v1.2";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PromptBuilder() {
    }

    public static String systemPrompt() {
        return "你是行至集（SINCIT）产业招商事业部的资深投标总监，熟悉政府与国企的招商代理、产业运营、"
                + "品牌策划类采购项目。请基于给定的项目信息、招标要素、匹配评估与报价测算，输出一份可直接进入"
                + "人工复核的中文标书初稿。要求：\n"
                + "1. 结构完整、逻辑清晰、语言专业克制，不夸大、不编造无法证明的资质与业绩；\n"
                + "2. 必须逐条响应招标文件中的硬性要求，无法满足的要在风险提示中明确写出；\n"
                + "3. 突出行至集的差异化能力：鼓楼国资参股背景、AAA 信用、ISO 三体系、80+ 中介机构、"
                + "1500 名合作经纪人、20 所高校、100 余家商协会资源；\n"
                + "4. 只输出 JSON，不要 Markdown 代码块，格式为："
                + "{\"outline\":[{\"title\":\"章节\",\"points\":[\"要点\"]}],"
                + "\"technicalProposal\":\"技术标正文\",\"commercialResponse\":\"商务响应\","
                + "\"qualificationResponse\":\"资格响应\",\"riskNotes\":[\"风险提示\"]}。";
    }

    public static String userPrompt(BidProject project, String emphasis, String tone) {
        StringBuilder sb = new StringBuilder();
        sb.append("【项目基本信息】\n");
        sb.append("项目名称：").append(nullSafe(project.getName())).append('\n');
        sb.append("招标人/委托方：").append(nullSafe(project.getClientName())).append('\n');
        sb.append("项目载体：").append(nullSafe(project.getCarrierName())).append('\n');
        sb.append("所属产业：").append(nullSafe(project.getIndustry())).append('\n');
        sb.append("项目所在地/部门：").append(nullSafe(project.getDepartment())).append('\n');
        sb.append("招标预算：").append(project.getBudgetAmount() == null ? "未提供"
                : project.getBudgetAmount().stripTrailingZeros().toPlainString() + " 万元").append('\n');
        sb.append("投标截止：").append(project.getDeadline() == null ? "未提供"
                : project.getDeadline().format(DATE_TIME)).append('\n');
        sb.append("项目背景：").append(nullSafe(project.getProjectBrief())).append("\n\n");

        List<RequirementItem> items = project.getExtraction() == null ? null : project.getExtraction().getItems();
        sb.append("【招标关键要素】\n");
        if (items == null || items.isEmpty()) {
            sb.append("暂无结构化要素，请依据项目背景合理编制，并在风险提示中说明需人工核对。\n");
        } else {
            int index = 1;
            for (RequirementItem item : items) {
                if (index > 12) {
                    break;
                }
                sb.append(index++).append(". [").append(item.getCategory()).append("] ")
                        .append(item.getContent());
                if (item.isMandatory()) {
                    sb.append("（硬性要求）");
                }
                sb.append('\n');
            }
        }

        if (project.getMatchReport() != null) {
            sb.append("\n【资质匹配评估】得分 ").append(project.getMatchReport().getScore())
                    .append("，建议：").append(project.getMatchReport().getRecommendation()).append('\n');
            sb.append("需补强：").append(String.join("；", project.getMatchReport().getGaps())).append('\n');
        }
        if (project.getPricingPlan() != null) {
            sb.append("\n【报价测算】建议报价 ").append(project.getPricingPlan().getRecommendedPrice())
                    .append(" 万元，毛利率 ").append(project.getPricingPlan().getGrossMarginRate())
                    .append("%，策略：").append(project.getPricingPlan().getStrategy()).append('\n');
        }
        sb.append("\n【编制要求】\n");
        sb.append("语气：").append(nullSafe(tone).isEmpty() ? "专业、稳健" : tone).append('\n');
        sb.append("重点：").append(nullSafe(emphasis).isEmpty() ? "产业招商策略、渠道资源、去化保障" : emphasis).append('\n');
        sb.append("请输出完整 JSON，技术标正文不少于 1800 字。");
        return sb.toString();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
