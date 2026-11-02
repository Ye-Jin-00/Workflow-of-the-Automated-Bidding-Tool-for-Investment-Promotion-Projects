package com.sincit.bid.config;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.domain.BidStatus;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.RuleDefaults;
import com.sincit.bid.domain.User;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.persistence.RuleRepository;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.service.NotificationService;
import com.sincit.bid.service.ProjectService;
import com.sincit.bid.service.WorkflowService;
import com.sincit.bid.web.dto.ProjectDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 演示数据初始化。首次启动时写入用户、规则与 6 个覆盖不同工作流状态的项目，
 * 让评审者无需造数即可看到“正常推进 / 自动驳回 / 复核退回 / 已归档 / 草稿”全链路。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter CN_DATE_TIME = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

    private final UserRepository userRepository;
    private final RuleRepository ruleRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final WorkflowService workflowService;
    private final NotificationService notificationService;

    private final CurrentUser manager = new CurrentUser("U002", "manager", "manager", UserRole.BID_MANAGER, "招商一部");
    private final CurrentUser reviewer = new CurrentUser("U003", "reviewer", "reviewer", UserRole.REVIEWER, "风控合规部");

    public DataInitializer(UserRepository userRepository, RuleRepository ruleRepository,
                           ProjectRepository projectRepository, ProjectService projectService,
                           WorkflowService workflowService, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.ruleRepository = ruleRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.workflowService = workflowService;
        this.notificationService = notificationService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedUsers();
            log.info("demo users created: admin / manager / reviewer / viewer");
        }
        if (ruleRepository.isEmpty()) {
            ruleRepository.saveAll(RuleDefaults.all());
            log.info("default qualification rules created: {}", RuleDefaults.all().size());
        }
        if (projectRepository.count() == 0) {
            seedProjects();
            log.info("demo projects created: {}", projectRepository.count());
        }
        notificationService.seedDemo();
    }

    private void seedUsers() {
        createUser("U001", "admin", "Admin@123", "admin", UserRole.ADMIN, "总经办", "13800000001");
        createUser("U002", "manager", "Bid@123", "manager", UserRole.BID_MANAGER, "招商一部", "13800000002");
        createUser("U003", "reviewer", "Review@123", "reviewer", UserRole.REVIEWER, "风控合规部", "13800000003");
        createUser("U004", "viewer", "View@123", "viewer", UserRole.VIEWER, "招商二部", "13800000004");
    }

    private void createUser(String id, String username, String password, String name,
                            UserRole role, String department, String phone) {
        String salt = com.sincit.bid.auth.PasswordHasher.newSalt();
        String title;
        switch (role) {
            case ADMIN:
                title = "系统管理员";
                break;
            case REVIEWER:
                title = "风控复核经理";
                break;
            case VIEWER:
                title = "招商观察员";
                break;
            default:
                title = "高级投标经理";
        }
        userRepository.save(User.builder()
                .id(id)
                .username(username)
                .salt(salt)
                .passwordHash(com.sincit.bid.auth.PasswordHasher.hash(password, salt))
                .displayName(name)
                .role(role)
                .department(department)
                .title(title)
                .phone(phone)
                .email(username + "@sincit-demo.com")
                .bio("演示账号：" + username + "，用于体验对应角色的权限。")
                .enabled(true)
                .tokenVersion(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void seedProjects() {
        // 1. 待人工复核：完整跑通资格预审、要素抽取、匹配测算、AI 初稿
        LocalDateTime d1 = deadline(18);
        ProjectDtos.Detail p1 = createProject(
                "电建中储泛悦城市广场 T2 写字楼招商代理服务",
                "电建中储（南京）置业有限公司", "泛悦城市广场 T2", "商务服务/总部经济", "政府推荐",
                "480", "10", d1,
                "项目位于鼓楼区小市北祥路，总建筑面积约 34 万㎡，其中 T2 写字楼 4.1 万㎡，"
                        + "2021 年 5 月入市。现需引入总部经济、商务服务、专业服务类企业，提升去化速度与租金水平，"
                        + "并要求与鼓楼区产业政策、国资资源形成协同。",
                tender("电建中储泛悦城市广场 T2 写字楼招商代理服务", "电建中储（南京）置业有限公司",
                        "480", "10", d1),
                QualificationProfile.demoDefault());
        p1 = qualify(p1);
        p1 = extract(p1, tenderText(p1));
        p1 = matchAndPrice(p1, 90, "1200", 3);
        p1 = draft(p1, "总部经济企业招引、国有企业资源协同、去化节奏控制");
        p1 = workflowService.submitReview(p1.getId(), p1.getVersion(), manager);
        assertStatus(p1, BidStatus.REVIEW_PENDING);

        // 2. 要素已抽取：等待匹配测算
        LocalDateTime d2 = deadline(25);
        ProjectDtos.Detail p2 = createProject(
                "仙林万达茂文旅商业综合体招商运营服务",
                "南京万达茂投资有限公司", "仙林万达茂", "文旅商业", "公共资源交易平台",
                "860", "17", d2,
                "项目位于仙林大道与守敬路交汇处，商业面积约 30 万㎡，包含万达茂商业、酒店与文旅业态。"
                        + "需完成文旅商业定位、品牌招商、开业筹备与持续运营，打造南京主城区文旅商业标杆。",
                tender("仙林万达茂文旅商业综合体招商运营服务", "南京万达茂投资有限公司", "860", "17", d2),
                QualificationProfile.demoDefault());
        p2 = qualify(p2);
        p2 = extract(p2, tenderText(p2));
        assertStatus(p2, BidStatus.REQUIREMENTS_EXTRACTED);

        // 3. 资格不满足：系统自动驳回（条件分支演示）
        LocalDateTime d3 = deadline(12);
        QualificationProfile weak = QualificationProfile.demoDefault();
        weak.setRegisteredCapital(new BigDecimal("50"));
        weak.setCreditLevel("A");
        weak.setSimilarProjectCount(0);
        weak.setSimilarProjectAmount(BigDecimal.ZERO);
        weak.setTeamSize(4);
        weak.setBidBondAvailable(false);
        weak.setQualifications(Arrays.asList("品牌策划服务"));
        ProjectDtos.Detail p3 = createProject(
                "鼓楼滨江 U-LIVE 商办载体产业招商服务",
                "南京中海地产有限公司", "鼓楼滨江 U-LIVE", "商务服务/总部经济", "渠道推荐",
                "360", "7", d3,
                "项目位于鼓楼区新兴路与复兴街交汇处东南，总建筑面积约 16 万㎡，为商办综合体。"
                        + "本次拟引入产业招商服务商，要求具备同类商办载体招商经验与产业资源。",
                tender("鼓楼滨江 U-LIVE 商办载体产业招商服务", "南京中海地产有限公司", "360", "7", d3),
                weak);
        p3 = qualify(p3);
        assertStatus(p3, BidStatus.QUALIFICATION_REJECTED);

        // 4. 已归档：完整闭环，含复核通过、提交回执与归档档号
        LocalDateTime d4 = deadline(40);
        ProjectDtos.Detail p4 = createProject(
                "壹城数字经济创意园品牌顾问服务",
                "南京壹城建设集团有限公司", "壹城数字经济创意园", "数字经济", "客户委托",
                "320", "6", d4,
                "项目位于秦淮区南部新城，总建筑面积约 11.8 万㎡，定位数字经济创意园区。"
                        + "需提供品牌定位、VI 体系、产业招商策略与年度品牌运营服务。",
                tender("壹城数字经济创意园品牌顾问服务", "南京壹城建设集团有限公司", "320", "6", d4),
                QualificationProfile.demoDefault());
        p4 = qualify(p4);
        p4 = extract(p4, tenderText(p4));
        p4 = matchAndPrice(p4, 70, "1300", 4);
        p4 = draft(p4, "数字经济产业品牌塑造、园区 IP 运营、招商转化");
        p4 = workflowService.submitReview(p4.getId(), p4.getVersion(), manager);
        p4 = review(p4, "APPROVE", "方案结构完整，品牌策略与产业定位匹配，报价在预算范围内，同意投标。");
        p4 = workflowService.submit(p4.getId(), submitRequest(p4, "NJGGZY-2026-0921-018", "电子招投标平台"), manager);
        p4 = archive(p4);
        assertStatus(p4, BidStatus.ARCHIVED);

        // 5. 复核退回：展示人工复核节点的退回分支与重新提交路径
        LocalDateTime d5 = deadline(22);
        ProjectDtos.Detail p5 = createProject(
                "中信泰富总部办公载体招商代理服务",
                "中信泰富（南京）投资有限公司", "中信泰富总部办公载体", "商务服务/总部经济", "老客户复购",
                "520", "10", d5,
                "项目为南京主城核心区总部办公载体，建筑面积约 8 万㎡，需引入金融、专业服务与总部型企业。"
                        + "招标人特别关注文旅商业资源导入与首年去化率。",
                tender("中信泰富总部办公载体招商代理服务", "中信泰富（南京）投资有限公司", "520", "10", d5),
                QualificationProfile.demoDefault());
        p5 = qualify(p5);
        p5 = extract(p5, tenderText(p5));
        p5 = matchAndPrice(p5, 80, "1100", 5);
        p5 = draft(p5, "总部型企业招引、金融资源导入、首年去化保障");
        p5 = workflowService.submitReview(p5.getId(), p5.getVersion(), manager);
        p5 = review(p5, "RETURN", "技术标缺少针对文旅商业资源的去化举措，首年去化目标缺少分月拆解，请补充后重新提交。");
        assertStatus(p5, BidStatus.REVIEW_REJECTED);

        // 6. 草稿：等待录入人完成资格预审
        LocalDateTime d6 = deadline(30);
        ProjectDtos.Detail p6 = createProject(
                "幕府创新区产业载体招商推介服务",
                "南京鼓楼幕府创新区开发建设有限公司", "幕府创新区产业载体", "科技服务", "自主开发",
                "260", "5", d6,
                "幕府创新区拟于近期开展产业载体集中招商推介，需服务商提供产业研究、活动策划、"
                        + "渠道邀约与项目对接服务。",
                tender("幕府创新区产业载体招商推介服务", "南京鼓楼幕府创新区开发建设有限公司", "260", "5", d6),
                QualificationProfile.demoDefault());
        assertStatus(p6, BidStatus.DRAFT);
    }

    private ProjectDtos.Detail createProject(String name, String client, String carrier, String industry,
                                              String source, String budget, String deposit, LocalDateTime deadline,
                                              String brief, String tenderText, QualificationProfile profile) {
        ProjectDtos.SaveRequest request = new ProjectDtos.SaveRequest();
        request.setName(name);
        request.setClientName(client);
        request.setCarrierName(carrier);
        request.setIndustry(industry);
        request.setSource(source);
        request.setDepartment("招商一部");
        request.setDeadline(deadline.format(DATE_TIME));
        request.setBudgetAmount(new BigDecimal(budget));
        request.setDepositAmount(new BigDecimal(deposit));
        request.setProjectBrief(brief);
        request.setTenderText(tenderText);
        request.setProfile(profile);
        return projectService.create(request, manager);
    }

    private ProjectDtos.Detail qualify(ProjectDtos.Detail detail) {
        ProjectDtos.QualificationRequest request = new ProjectDtos.QualificationRequest();
        request.setProfile(detail.getProfile());
        request.setVersion(detail.getVersion());
        return workflowService.qualificationCheck(detail.getId(), request, manager);
    }

    private ProjectDtos.Detail extract(ProjectDtos.Detail detail, String text) {
        ProjectDtos.ExtractRequest request = new ProjectDtos.ExtractRequest();
        request.setTenderText(text);
        request.setVersion(detail.getVersion());
        return workflowService.extract(detail.getId(), request, manager);
    }

    private ProjectDtos.Detail matchAndPrice(ProjectDtos.Detail detail, int personDays, String dailyRate, int competitors) {
        ProjectDtos.MatchPriceRequest request = new ProjectDtos.MatchPriceRequest();
        request.setPersonDays(personDays);
        request.setDailyRate(new BigDecimal(dailyRate));
        request.setCompetitorCount(competitors);
        request.setVersion(detail.getVersion());
        return workflowService.matchAndPrice(detail.getId(), request, manager);
    }

    private ProjectDtos.Detail draft(ProjectDtos.Detail detail, String emphasis) {
        ProjectDtos.AiDraftRequest request = new ProjectDtos.AiDraftRequest();
        request.setEmphasis(emphasis);
        request.setTone("专业、稳健");
        request.setVersion(detail.getVersion());
        return workflowService.generateDraft(detail.getId(), request, manager);
    }

    private ProjectDtos.Detail review(ProjectDtos.Detail detail, String decision, String opinion) {
        ProjectDtos.ReviewRequest request = new ProjectDtos.ReviewRequest();
        request.setDecision(decision);
        request.setOpinion(opinion);
        request.setVersion(detail.getVersion());
        return workflowService.review(detail.getId(), request, reviewer);
    }

    private ProjectDtos.SubmitRequest submitRequest(ProjectDtos.Detail detail, String referenceNo, String method) {
        ProjectDtos.SubmitRequest request = new ProjectDtos.SubmitRequest();
        request.setReferenceNo(referenceNo);
        request.setMethod(method);
        request.setNote("已完成电子签章与标书上传，回执已归档。");
        request.setVersion(detail.getVersion());
        return request;
    }

    private ProjectDtos.Detail archive(ProjectDtos.Detail detail) {
        ProjectDtos.ArchiveRequest request = new ProjectDtos.ArchiveRequest();
        request.setArchiveNo("ARC-" + detail.getCode());
        request.setRetentionYears(10);
        request.setNote("投标文件、评审记录、回执与复盘材料已归档。");
        request.setVersion(detail.getVersion());
        return workflowService.archive(detail.getId(), request, manager);
    }

    private void assertStatus(ProjectDtos.Detail detail, BidStatus expected) {
        if (!expected.name().equals(detail.getStatus())) {
            throw new IllegalStateException("演示数据初始化失败：" + detail.getCode() + " 期望状态 "
                    + expected + "，实际 " + detail.getStatus());
        }
    }

    private LocalDateTime deadline(int days) {
        return LocalDateTime.now().plusDays(days).withHour(17).withMinute(0).withSecond(0).withNano(0);
    }

    private String tenderText(ProjectDtos.Detail detail) {
        return tender(detail.getName(), detail.getClientName(),
                detail.getBudgetAmount().stripTrailingZeros().toPlainString(),
                detail.getDepositAmount().stripTrailingZeros().toPlainString(),
                detail.getDeadline());
    }

    private String tender(String name, String client, String budget, String deposit, LocalDateTime deadline) {
        return "项目名称：" + name + "\n"
                + "招标人：" + client + "\n"
                + "预算金额：人民币" + budget + "万元\n"
                + "投标保证金：人民币" + deposit + "万元\n"
                + "投标截止时间：" + deadline.format(CN_DATE_TIME) + "\n"
                + "一、资格要求\n"
                + "1. 投标人须具备房地产经纪机构备案或招商运营服务能力，注册资本不低于300万元。\n"
                + "2. 近三年须具有不少于2个同类项目业绩，并提供合同关键页及验收证明。\n"
                + "3. 信用等级AA级及以上，且无失信被执行、重大税收违法记录。\n"
                + "4. 拟投入项目团队不少于10人，须配备项目负责人、招商经理与运营经理。\n"
                + "二、服务要求\n"
                + "5. 负责产业定位、招商策略制定、渠道组织、企业招引与落地服务。\n"
                + "6. 合作期内完成可招商面积60%以上去化，并引入不少于3家产业链关键企业。\n"
                + "7. 每月提交经营分析报告，每季度组织不少于1场招商推介活动。\n"
                + "三、商务要求\n"
                + "8. 投标保证金须在截止时间前足额缴纳，未缴纳按无效标处理。\n"
                + "9. 付款方式：合同签订后支付30%，中期验收后支付40%，服务期满验收合格后支付30%。\n"
                + "10. 报价须包含人工、渠道、活动执行、差旅与税费等全部费用。\n"
                + "四、评标办法\n"
                + "11. 采用综合评分法：价格分30分、技术分50分、商务分20分。\n"
                + "12. 服务期12个月，核心团队须驻场服务，未经招标人同意不得更换。\n";
    }
}
