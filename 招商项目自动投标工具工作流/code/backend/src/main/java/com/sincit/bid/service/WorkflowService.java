package com.sincit.bid.service;

import com.sincit.bid.ai.AiDraftService;
import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.AiDraft;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.BidStatus;
import com.sincit.bid.domain.ExtractionResult;
import com.sincit.bid.domain.MatchReport;
import com.sincit.bid.domain.PricingPlan;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.QualificationResult;
import com.sincit.bid.domain.ReviewRecord;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工作流编排：资格预审 -> 要素抽取 -> 匹配测算 -> AI 初稿 -> 人工复核 -> 提交 -> 归档。
 * 每个动作都做状态、权限、版本三重校验，成功后统一记录审计日志。
 */
@Service
public class WorkflowService {

    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final QualificationService qualificationService;
    private final ExtractionService extractionService;
    private final MatchService matchService;
    private final PricingService pricingService;
    private final AiDraftService aiDraftService;
    private final AuditService auditService;
    private final PermissionService permissionService;
    private final NotificationService notificationService;

    public WorkflowService(ProjectRepository projectRepository, ProjectService projectService,
                           QualificationService qualificationService, ExtractionService extractionService,
                           MatchService matchService, PricingService pricingService,
                           AiDraftService aiDraftService, AuditService auditService,
                           PermissionService permissionService, NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.qualificationService = qualificationService;
        this.extractionService = extractionService;
        this.matchService = matchService;
        this.pricingService = pricingService;
        this.aiDraftService = aiDraftService;
        this.auditService = auditService;
        this.permissionService = permissionService;
        this.notificationService = notificationService;
    }

    /**
     * 资格预审：硬性条件不满足时自动驳回（条件分支），只预警时允许继续但必须复核说明。
     */
    public ProjectDtos.Detail qualificationCheck(String id, ProjectDtos.QualificationRequest request,
                                                 CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        projectService.ensureReady(project);
        assertStatus(project, "资格预审", BidStatus.DRAFT, BidStatus.QUALIFICATION_REJECTED,
                BidStatus.QUALIFICATION_PASSED);
        projectService.requireVersion(request.getVersion(), project);

        QualificationProfile profile = request.getProfile() == null ? project.getProfile() : request.getProfile();
        QualificationResult result = qualificationService.evaluate(project, profile);

        project.setProfile(profile);
        project.setQualificationResult(result);
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        if (result.isPassed()) {
            project.setStatus(BidStatus.QUALIFICATION_PASSED);
            project.setRejectionReason(null);
            projectRepository.save(project);
            auditService.record(actor, project, AuditAction.QUALIFICATION_CHECK,
                    "预审通过，得分 " + result.getScore() + "，结论 " + result.getConclusion()
                            + (result.getWarnings().isEmpty() ? "" : "；预警 " + result.getWarnings().size() + " 项"));
        } else {
            project.setStatus(BidStatus.QUALIFICATION_REJECTED);
            project.setRejectionReason(String.join("；", result.getHardFailures()));
            projectRepository.save(project);
            auditService.record(actor, project, AuditAction.AUTO_REJECT,
                    "资格预审不满足 " + result.getHardFailures().size() + " 项必填条件，系统自动驳回："
                            + truncate(String.join("；", result.getHardFailures()), 300));
        }
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail extract(String id, ProjectDtos.ExtractRequest request, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        assertStatus(project, "要素抽取", BidStatus.QUALIFICATION_PASSED, BidStatus.REQUIREMENTS_EXTRACTED);
        projectService.requireVersion(request.getVersion(), project);

        String tenderText = request.getTenderText() != null ? request.getTenderText() : project.getTenderText();
        ExtractionResult result = extractionService.extract(project, tenderText);

        project.setTenderText(tenderText);
        project.setExtraction(result);
        if (project.getBudgetAmount() == null && result.getBudgetAmount() != null) {
            project.setBudgetAmount(result.getBudgetAmount());
        }
        if (project.getDepositAmount() == null && result.getDepositAmount() != null) {
            project.setDepositAmount(result.getDepositAmount());
        }
        if (project.getDeadline() == null && result.getDeadline() != null) {
            project.setDeadline(result.getDeadline());
        }
        project.setStatus(BidStatus.REQUIREMENTS_EXTRACTED);
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.EXTRACT,
                "抽取要素 " + result.getItems().size() + " 项，硬性要求 "
                        + result.getItems().stream().filter(i -> i.isMandatory()).count()
                        + " 项；" + (result.getWarnings().isEmpty() ? "无异常" : "预警：" + String.join("；", result.getWarnings())));
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail matchAndPrice(String id, ProjectDtos.MatchPriceRequest request, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        assertStatus(project, "匹配与报价测算", BidStatus.REQUIREMENTS_EXTRACTED, BidStatus.MATCHED);
        projectService.requireVersion(request.getVersion(), project);

        MatchReport matchReport = matchService.evaluate(project);
        PricingPlan pricingPlan = pricingService.calculate(project, request);

        project.setMatchReport(matchReport);
        project.setPricingPlan(pricingPlan);
        project.setBidAmount(pricingPlan.getRecommendedPrice());
        project.setStatus(BidStatus.MATCHED);
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.MATCH_PRICE,
                "匹配得分 " + matchReport.getScore() + "（" + matchReport.getRecommendation()
                        + "），建议报价 " + pricingPlan.getRecommendedPrice() + " 万元，毛利率 "
                        + pricingPlan.getGrossMarginRate() + "%");
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail generateDraft(String id, ProjectDtos.AiDraftRequest request, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        assertStatus(project, "AI 生成初稿", BidStatus.MATCHED, BidStatus.AI_DRAFTED, BidStatus.REVIEW_REJECTED);
        projectService.requireVersion(request.getVersion(), project);

        AiDraft draft = aiDraftService.generate(project, request.getEmphasis(), request.getTone());
        project.setAiDraft(draft);
        project.setStatus(BidStatus.AI_DRAFTED);
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.AI_DRAFT,
                "生成标书初稿，供应商 " + draft.getProvider() + "，模型 " + draft.getModel()
                        + "，耗时 " + draft.getElapsedMs() + "ms"
                        + (draft.isDegraded() ? "（降级生成）" : ""));
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail updateDraft(String id, ProjectDtos.DraftUpdateRequest request, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        assertStatus(project, "编辑初稿", BidStatus.AI_DRAFTED, BidStatus.REVIEW_REJECTED);
        projectService.requireVersion(request.getVersion(), project);
        if (project.getAiDraft() == null) {
            throw BusinessException.conflict("尚未生成初稿，无法编辑");
        }
        AiDraft draft = project.getAiDraft();
        if (request.getOutline() != null) {
            draft.setOutline(request.getOutline());
        }
        if (request.getTechnicalProposal() != null) {
            if (request.getTechnicalProposal().trim().isEmpty()) {
                throw BusinessException.invalid("技术标正文不能为空");
            }
            draft.setTechnicalProposal(request.getTechnicalProposal());
        }
        if (request.getCommercialResponse() != null) {
            draft.setCommercialResponse(request.getCommercialResponse());
        }
        if (request.getQualificationResponse() != null) {
            draft.setQualificationResponse(request.getQualificationResponse());
        }
        if (request.getRiskNotes() != null) {
            draft.setRiskNotes(request.getRiskNotes());
        }
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.DRAFT_UPDATE, "人工编辑标书初稿");
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail submitReview(String id, Integer version, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        assertStatus(project, "提交复核", BidStatus.AI_DRAFTED, BidStatus.REVIEW_REJECTED);
        projectService.requireVersion(version, project);
        if (project.getAiDraft() == null || project.getAiDraft().getTechnicalProposal() == null) {
            throw BusinessException.conflict("初稿内容为空，无法提交复核");
        }
        if (project.getDeadline() != null && project.getDeadline().isBefore(LocalDateTime.now())) {
            throw BusinessException.conflict("投标截止时间已过，无法提交复核");
        }
        project.setStatus(BidStatus.REVIEW_PENDING);
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.SUBMIT_REVIEW,
                "提交人工复核，当前为第 " + (maxRound(project) + 1) + " 轮");
        notificationService.notifyReviewTask(project, actor);
        return projectService.toDetail(project, actor);
    }

    /**
     * 人工复核：通过 -> 待提交；退回 -> 回到可编辑状态，保留全部历史意见。
     */
    public ProjectDtos.Detail review(String id, ProjectDtos.ReviewRequest request, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertReview(actor, project);
        assertStatus(project, "人工复核", BidStatus.REVIEW_PENDING);
        projectService.requireVersion(request.getVersion(), project);

        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toUpperCase();
        ReviewRecord.ReviewDecision reviewDecision;
        if ("APPROVE".equals(decision)) {
            reviewDecision = ReviewRecord.ReviewDecision.APPROVE;
        } else if ("RETURN".equals(decision)) {
            reviewDecision = ReviewRecord.ReviewDecision.RETURN;
        } else {
            throw BusinessException.invalid("复核结论必须为 APPROVE 或 RETURN");
        }
        String opinion = request.getOpinion() == null ? "" : request.getOpinion().trim();
        if (reviewDecision == ReviewRecord.ReviewDecision.RETURN && opinion.length() < 5) {
            throw BusinessException.invalid("退回时必须填写不少于 5 个字的修改意见");
        }
        if (opinion.isEmpty()) {
            opinion = "同意，按方案执行。";
        }

        int round = maxRound(project) + 1;
        ReviewRecord record = ReviewRecord.builder()
                .id("RV" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .round(round)
                .reviewerId(actor.getId())
                .reviewerName(actor.getDisplayName())
                .decision(reviewDecision)
                .opinion(opinion)
                .createdAt(LocalDateTime.now())
                .build();
        project.getReviews().add(record);
        project.setLastReviewOpinion(opinion);
        if (reviewDecision == ReviewRecord.ReviewDecision.APPROVE) {
            project.setStatus(BidStatus.APPROVED);
        } else {
            project.setStatus(BidStatus.REVIEW_REJECTED);
        }
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project,
                reviewDecision == ReviewRecord.ReviewDecision.APPROVE ? AuditAction.REVIEW_APPROVE : AuditAction.REVIEW_RETURN,
                "第 " + round + " 轮复核：" + reviewDecision.getLabel() + "；意见：" + truncate(opinion, 200));
        notificationService.markReviewTaskRead(project);
        notificationService.notifyReviewResult(project, actor,
                reviewDecision == ReviewRecord.ReviewDecision.APPROVE, opinion);
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail submit(String id, ProjectDtos.SubmitRequest request, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        assertStatus(project, "提交投标", BidStatus.APPROVED);
        projectService.requireVersion(request.getVersion(), project);
        if (project.getDeadline() != null && project.getDeadline().isBefore(LocalDateTime.now())) {
            throw BusinessException.conflict("投标截止时间已过，无法提交");
        }
        String referenceNo = request.getReferenceNo() == null ? "" : request.getReferenceNo().trim();
        if (referenceNo.length() < 3) {
            throw BusinessException.invalid("请填写投标平台回执号/递交编号（至少 3 个字符）");
        }
        if (project.getBidAmount() == null) {
            throw BusinessException.conflict("尚未完成报价测算，无法提交");
        }
        project.setSubmission(BidProject.Submission.builder()
                .submittedAt(LocalDateTime.now())
                .method(request.getMethod() == null || request.getMethod().trim().isEmpty()
                        ? "电子招投标平台" : request.getMethod().trim())
                .referenceNo(referenceNo)
                .operatorName(actor.getDisplayName())
                .note(request.getNote() == null ? null : request.getNote().trim())
                .build());
        project.setStatus(BidStatus.SUBMITTED);
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.SUBMIT,
                "提交投标，方式：" + project.getSubmission().getMethod() + "，回执号：" + referenceNo);
        notificationService.notifyStatus(project, actor);
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail archive(String id, ProjectDtos.ArchiveRequest request, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        assertStatus(project, "归档", BidStatus.SUBMITTED);
        projectService.requireVersion(request.getVersion(), project);
        int retentionYears = request.getRetentionYears() == null ? 5 : request.getRetentionYears();
        if (retentionYears < 1 || retentionYears > 30) {
            throw BusinessException.invalid("保存年限需在 1-30 年之间");
        }
        String archiveNo = request.getArchiveNo() == null || request.getArchiveNo().trim().isEmpty()
                ? "ARC-" + project.getCode() : request.getArchiveNo().trim();
        project.setArchive(BidProject.Archive.builder()
                .archivedAt(LocalDateTime.now())
                .archiveNo(archiveNo)
                .retentionYears(retentionYears)
                .operatorName(actor.getDisplayName())
                .note(request.getNote() == null ? null : request.getNote().trim())
                .build());
        project.setStatus(BidStatus.ARCHIVED);
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.ARCHIVE,
                "项目归档，档号：" + archiveNo + "，保存 " + retentionYears + " 年");
        notificationService.notifyStatus(project, actor);
        return projectService.toDetail(project, actor);
    }

    private void assertStatus(BidProject project, String action, BidStatus... allowed) {
        for (BidStatus status : allowed) {
            if (project.getStatus() == status) {
                return;
            }
        }
        throw BusinessException.conflict("当前状态为“" + project.getStatus().getLabel() + "”，不允许执行“" + action + "”");
    }

    private int maxRound(BidProject project) {
        return project.getReviews() == null ? 0
                : project.getReviews().stream().mapToInt(ReviewRecord::getRound).max().orElse(0);
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
