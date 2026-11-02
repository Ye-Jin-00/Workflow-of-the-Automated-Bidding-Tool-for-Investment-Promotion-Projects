package com.sincit.bid.ai;

import com.sincit.bid.domain.AiDraft;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.CompanyCapabilities;
import com.sincit.bid.domain.RequirementItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地模板生成器：不依赖外部大模型即可产出结构完整、可人工编辑的标书初稿。
 * 它同时承担大模型不可用时的降级兜底，保证工作流永不因外部服务中断而卡死。
 */
@Component
public class LocalTemplateAiProvider implements AiProvider {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    @Override
    public String name() {
        return "local-template";
    }

    @Override
    public AiDraft generate(BidProject project, String emphasis, String tone) {
        String projectName = safe(project.getName(), "招商项目");
        String carrier = safe(project.getCarrierName(), "项目载体");
        String industry = safe(project.getIndustry(), "区域重点产业");
        String client = safe(project.getClientName(), "招标人");
        String budget = project.getBudgetAmount() == null ? "以招标文件为准"
                : project.getBudgetAmount().stripTrailingZeros().toPlainString() + " 万元";
        String deadline = project.getDeadline() == null ? "以招标文件为准" : project.getDeadline().format(DATE);
        String price = project.getPricingPlan() == null || project.getPricingPlan().getRecommendedPrice() == null
                ? "以商务报价表为准"
                : project.getPricingPlan().getRecommendedPrice().stripTrailingZeros().toPlainString() + " 万元";
        String margin = project.getPricingPlan() == null ? "-"
                : String.valueOf(project.getPricingPlan().getGrossMarginRate());
        String focus = safe(emphasis, "产业招商策略、渠道资源与去化保障");
        String toneText = safe(tone, "专业、稳健");

        StringBuilder tech = new StringBuilder();
        tech.append("一、项目理解与总体思路\n")
                .append("行至集对本项目的理解是：").append(projectName).append("由").append(client)
                .append("发起，载体为").append(carrier).append("，产业方向聚焦").append(industry)
                .append("，招标预算约").append(budget).append("，投标截止时间为").append(deadline)
                .append("。项目不仅要求完成空间去化，更要求在产业定位、企业招引、落地服务与区域品牌之间形成闭环。\n")
                .append("当前招商工作普遍面临三方面挑战：一是产业定位同质化，容易陷入“什么企业都招”的被动局面；")
                .append("二是渠道触达碎片化，中介、商协会、高校与政府资源之间缺少统一调度；")
                .append("三是签约与落地脱节，企业注册、政策申报、办公交付等环节衔接不畅，影响去化质量与口碑。\n")
                .append("基于上述判断，我方提出“产业研究先行、渠道组织制胜、运营服务兜底”的总体思路：")
                .append("以区域产业基础和载体禀赋确定 2—3 个主攻赛道，建立目标企业库；")
                .append("以行至集渠道网络与活动能力实现批量触达和精准转化；")
                .append("以企业成长服务体系保障“招得来、留得住、长得大”。\n")
                .append("本方案编制侧重：").append(focus).append("；整体语气：").append(toneText).append("。\n\n");

        tech.append("二、产业定位与招商目标\n")
                .append("（一）产业定位。围绕").append(industry).append("，结合载体硬件条件与周边产业基础，")
                .append("建议将目标企业划分为“链主及区域总部、高成长科技企业、专业服务机构”三类，")
                .append("分别匹配不同的面积段、租金策略与政策工具，避免单一业态过度集中。\n")
                .append("（二）招商目标（建议值，最终以合同约定为准）。合作期内完成载体可招商面积的 60% 以上去化；")
                .append("引入不少于 3 家产业链关键企业或区域总部；")
                .append("形成不少于 2 个可复制的产业招商案例；")
                .append("企业注册率、实际入驻率与租金收缴率纳入月度考核。\n")
                .append("（三）目标企业画像。重点筛选符合区域产业导向、具备稳定现金流与扩张需求、")
                .append("且与现有产业链形成协同的企业，建立“一企一档”，持续跟踪其选址周期与决策链。\n\n");

        tech.append("三、招商策略与渠道组合\n")
                .append("（一）产业链招商。绘制产业链图谱，识别缺失环节与关键配套，围绕链主企业开展上下游定向招引，")
                .append("以“龙头+配套”的方式提升签约转化率与产业集聚度。\n")
                .append("（二）渠道招商。依托行至集").append(CompanyCapabilities.AGENCIES).append("、")
                .append(CompanyCapabilities.BROKERS).append("，建立渠道分级管理与佣金结算机制；")
                .append("对渠道推荐项目实行 48 小时响应、72 小时带看、每周复盘，确保线索不流失。\n")
                .append("（三）活动招商。结合项目节点组织招商推介会、产业沙龙、企业开放日与线上直播看房，")
                .append("形成“月月有活动、季季有主题”的节奏；活动前明确邀约名单与转化目标，活动后 7 天内完成回访。\n")
                .append("（四）以商招商。通过已入驻企业、商协会与合作伙伴转介绍，降低获客成本；")
                .append("依托").append(CompanyCapabilities.ASSOCIATIONS).append("扩展触达面。\n")
                .append("（五）政策协同。梳理区域产业扶持、人才、税收与装修补贴政策，形成“政策明白卡”，")
                .append("在谈判阶段同步给出落地成本测算，增强企业决策信心。\n")
                .append("（六）数字招商。建立线索管理系统，对线索来源、跟进阶段、带看次数、报价与签约概率进行可视化跟踪，")
                .append("用数据驱动周度策略调整。\n\n");

        tech.append("四、实施计划与团队配置\n")
                .append("（一）实施计划。筹备期（第 1—4 周）：完成产业研究、载体梳理、价格与政策工具包、渠道动员；")
                .append("启动期（第 5—8 周）：集中邀约与推介活动，形成首批意向客户；")
                .append("攻坚期（第 9—24 周）：重点客户一对一谈判，推动签约与注册落地；")
                .append("运营期（第 25—52 周）：持续去化、企业服务与案例沉淀。\n")
                .append("（二）团队配置。拟投入项目总监 1 名、招商经理 3 名、渠道经理 2 名、运营服务 2 名、")
                .append("品牌策划 1 名、数据分析 1 名，后台由公司渠道中心与产业研究中心提供支持。")
                .append("项目总监对结果负责，实行周报、月度经营分析与季度复盘机制。\n")
                .append("（三）协作机制。与招标人建立联合工作组，明确双方接口人、决策流程与例会制度；")
                .append("重大价格与条款调整须经双方书面确认，确保过程可追溯。\n\n");

        tech.append("五、运营服务与去化保障\n")
                .append("（一）企业落地服务。提供选址、注册、装修、政策申报、人才招聘对接等一站式服务，")
                .append("缩短企业从签约到投产的周期。\n")
                .append("（二）企业成长服务。依托行至集“悦集荟”企业成长平台，组织财务、法务、人力资源、")
                .append("市场拓展等主题沙龙，提升企业黏性与续租率。\n")
                .append("（三）运营品质保障。协同物业建立企业诉求响应机制，明确报修、投诉、活动的响应时限；")
                .append("定期开展企业满意度调查，将结果纳入运营考核。\n")
                .append("（四）去化保障。建立“线索—意向—带看—报价—签约—入驻”全流程漏斗，")
                .append("对连续两周无进展的客户启动升级处理；对重点客户提供定制化落地方案。\n\n");

        tech.append("六、质量保障与风险控制\n")
                .append("（一）质量体系。行至集已通过").append(CompanyCapabilities.CERTIFICATIONS)
                .append("，项目执行按照体系文件进行过程控制与记录管理。\n")
                .append("（二）考核机制。以去化面积、签约金额、入驻率、企业满意度为核心指标，")
                .append("按月考核、按季兑现，未达节点及时纠偏。\n")
                .append("（三）风险控制。重点防范去化不达预期、同质化竞争、价格体系失控、")
                .append("企业注册地与税收贡献不匹配、渠道飞单等风险，逐项制定应对措施与责任人。\n")
                .append("（四）合规与保密。严格遵守招标人保密要求，项目数据分级授权、留痕管理；")
                .append("所有对外宣传口径须经招标人确认。\n\n");

        tech.append("七、资格与业绩响应\n")
                .append("行至集为").append(CompanyCapabilities.BACKDROP).append("，获评")
                .append(CompanyCapabilities.CREDIT).append("，并通过").append(CompanyCapabilities.CERTIFICATIONS)
                .append("。公司核心业务涵盖").append(CompanyCapabilities.SERVICE_SCOPE).append("。\n")
                .append("近年服务案例包括电建中储泛悦城市广场、铂丽大厦、仙林万达茂、鼓楼中海滨江 U-LIVE、")
                .append("壹城数字经济创意园等项目，覆盖商办综合体、文旅商业与产业园区等多种载体类型。")
                .append("公司具备完整的招商代理、品牌运营与企业服务能力，可为本项目提供持续支撑。\n\n");

        tech.append("八、针对招标文件硬性要求的响应说明\n")
                .append(mandatoryResponse(project)).append('\n');

        AiDraft.Section s1 = section("项目理解与总体思路",
                "产业定位同质化、渠道碎片化、签约与落地脱节是本项目三大难点",
                "总体思路：产业研究先行、渠道组织制胜、运营服务兜底",
                "编制侧重：" + focus);
        AiDraft.Section s2 = section("产业定位与招商目标",
                "目标企业分为链主及区域总部、高成长科技企业、专业服务机构三类",
                "合作期内去化 60% 以上，引入不少于 3 家关键企业",
                "建立一企一档，跟踪选址周期与决策链");
        AiDraft.Section s3 = section("招商策略与渠道组合",
                "产业链招商、渠道招商、活动招商、以商招商、政策协同、数字招商",
                "80+ 中介机构、1500 名经纪人、100 余家商协会资源",
                "线索 48 小时响应、72 小时带看、每周复盘");
        AiDraft.Section s4 = section("实施计划与团队配置",
                "四阶段推进：筹备、启动、攻坚、运营",
                "项目总监 + 招商 + 渠道 + 运营 + 品牌 + 数据",
                "周报、月度经营分析、季度复盘");
        AiDraft.Section s5 = section("运营服务与去化保障",
                "选址注册、政策申报、人才对接一站式落地",
                "悦集荟企业成长平台提升黏性",
                "全流程漏斗与重点客户升级机制");
        AiDraft.Section s6 = section("质量保障与风险控制",
                "ISO 三体系过程控制",
                "去化、签约、入驻、满意度四维考核",
                "保密、合规与数据分级授权");
        AiDraft.Section s7 = section("资格与业绩响应",
                "鼓楼国资参股、AAA 信用、ISO 三体系",
                "泛悦城市广场、铂丽大厦、仙林万达茂、中海滨江 U-LIVE 等案例",
                "覆盖商办、文旅商业与产业园区");
        AiDraft.Section s8 = section("硬性要求逐条响应",
                "招标文件硬性条款逐条对应",
                "不满足项在风险提示中明示",
                "证明材料按资格文件清单归档");

        List<AiDraft.Section> outline = new ArrayList<AiDraft.Section>();
        outline.add(s1);
        outline.add(s2);
        outline.add(s3);
        outline.add(s4);
        outline.add(s5);
        outline.add(s6);
        outline.add(s7);
        outline.add(s8);

        String commercial = "商务响应与报价说明\n"
                + "1. 投标报价：建议报价 " + price + "，最终以盖章报价表为准；报价包含人工、渠道、"
                + "活动执行、差旅、税费等完成本项目服务所需的全部费用。\n"
                + "2. 付款方式：建议按“合同签订后 30%、中期验收后 40%、服务期满验收合格后 30%”分期支付，"
                + "具体以合同谈判结果为准。\n"
                + "3. 服务期限：自合同签订之日起 12 个月，可根据去化进度协商延长。\n"
                + "4. 服务承诺：关键节点未达成的，按合同约定承担整改与违约责任；"
                + "项目团队核心成员未经招标人同意不随意更换。\n"
                + "5. 增值服务：免费提供产业研究报告、政策汇编、招商推介会策划及企业服务活动。\n"
                + "6. 成本说明：按当前测算，项目毛利率约 " + margin + "%，我方将在保证交付质量的前提下控制成本。\n";

        String qualification = "资格响应\n"
                + "1. 主体资格：行至集为依法注册的独立法人，具备承担本项目的能力。\n"
                + "2. 信用记录：获评 AAA 级信用企业，无失信被执行、重大税收违法与政府采购严重违法记录。\n"
                + "3. 资质认证：通过 ISO9001 质量管理体系、ISO14001 环境管理体系、ISO45001 职业健康安全管理体系认证。\n"
                + "4. 同类业绩：近三年完成多个商办、文旅商业及产业园区招商代理/品牌运营项目，详见业绩证明材料。\n"
                + "5. 团队能力：可投入具有产业研究、渠道组织、企业服务经验的专业团队，核心成员稳定。\n"
                + "6. 关联关系：与招标人不存在可能影响公正性的关联关系，并出具无利益冲突承诺函。\n";

        List<String> risks = new ArrayList<String>();
        if (project.getMatchReport() != null) {
            risks.addAll(project.getMatchReport().getGaps());
        }
        if (project.getPricingPlan() != null) {
            risks.addAll(project.getPricingPlan().getRiskNotes());
        }
        if (project.getDeadline() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), project.getDeadline());
            if (days < 5) {
                risks.add("距投标截止仅 " + days + " 天，需优先完成盖章、公证与电子标书上传演练");
            }
        }
        if (risks.isEmpty()) {
            risks.add("未发现重大风险，仍建议复核报价口径、业绩证明有效期与授权委托书签署情况");
        }

        return AiDraft.builder()
                .outline(outline)
                .technicalProposal(tech.toString())
                .commercialResponse(commercial)
                .qualificationResponse(qualification)
                .riskNotes(risks)
                .provider(name())
                .model("local-template-v1")
                .promptVersion(PromptBuilder.PROMPT_VERSION)
                .generatedAt(LocalDateTime.now())
                .degraded(false)
                .build();
    }

    private String mandatoryResponse(BidProject project) {
        if (project.getExtraction() == null || project.getExtraction().getItems().isEmpty()) {
            return "暂未抽取到结构化招标要素，建议先完成要素抽取后再生成响应表。";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (RequirementItem item : project.getExtraction().getItems()) {
            if (!item.isMandatory() || index > 8) {
                continue;
            }
            sb.append(index++).append(". ").append(item.getContent()).append('\n')
                    .append("   响应：已纳入本方案第").append(Math.min(index, 8)).append("节，")
                    .append("并在资格文件/技术文件中提供对应说明与证明材料。\n");
        }
        if (sb.length() == 0) {
            return "招标文件未标注明显硬性条款，我方仍按最严口径响应全部要求。";
        }
        return sb.toString();
    }

    private AiDraft.Section section(String title, String... points) {
        List<String> list = new ArrayList<String>();
        for (String point : points) {
            list.add(point);
        }
        return AiDraft.Section.builder().title(title).points(list).build();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
