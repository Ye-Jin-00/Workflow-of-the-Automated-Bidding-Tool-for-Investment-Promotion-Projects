package com.sincit.bid.service;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.common.DateUtils;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.BidStatus;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.ReviewRecord;
import com.sincit.bid.domain.User;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.web.dto.PageResult;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目读写与视图组装。权限判定统一走 PermissionService，返回值中附带 allowedActions，
 * 前端据此渲染按钮，后端在 WorkflowService 中仍会二次校验，避免“只靠前端隐藏按钮”的伪安全。
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
                          PermissionService permissionService, AuditService auditService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    public ProjectDtos.Detail create(ProjectDtos.SaveRequest request, CurrentUser actor) {
        permissionService.assertCanCreate(actor);
        String ownerId = actor.getId();
        String ownerName = actor.getDisplayName();
        String department = actor.getDepartment();
        if ((actor.isAdmin() || actor.getRole() == UserRole.BID_MANAGER)
                && request.getOwnerId() != null && !request.getOwnerId().trim().isEmpty()) {
            User owner = resolveOwner(request.getOwnerId(), actor);
            ownerId = owner.getId();
            ownerName = owner.getDisplayName();
            department = owner.getDepartment();
        }
        if (request.getDepartment() != null && !request.getDepartment().trim().isEmpty() && actor.isAdmin()) {
            department = request.getDepartment().trim();
        }

        LocalDateTime now = LocalDateTime.now();
        BidProject project = BidProject.builder()
                .id("P" + System.currentTimeMillis())
                .code(projectRepository.nextCode())
                .name(draftValue(request.getName(), "未命名草稿"))
                .clientName(draftValue(request.getClientName(), "待补充"))
                .carrierName(trim(request.getCarrierName()))
                .industry(trim(request.getIndustry()))
                .source(trim(request.getSource()))
                .ownerId(ownerId)
                .ownerName(ownerName)
                .department(department)
                .status(BidStatus.DRAFT)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor.getDisplayName())
                .deadline(DateUtils.parseDateTime(request.getDeadline()))
                .budgetAmount(request.getBudgetAmount())
                .depositAmount(request.getDepositAmount())
                .projectBrief(trim(request.getProjectBrief()))
                .tenderText(trim(request.getTenderText()))
                .profile(request.getProfile() == null ? QualificationProfile.demoDefault() : request.getProfile())
                .build();
        if (project.getProfile() != null && project.getProfile().getCompanyName() == null) {
            project.getProfile().setCompanyName("南京市行至集商业服务有限公司");
        }
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.PROJECT_CREATE,
                "创建投标项目：" + project.getName() + "，预算 " + amountText(project.getBudgetAmount()) + " 万元");
        return detail(project.getId(), actor);
    }

    public ProjectDtos.Detail update(String id, ProjectDtos.SaveRequest request, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        if (!project.getStatus().isBaseEditable()) {
            throw BusinessException.conflict("项目已进入“" + project.getStatus().getLabel() + "”阶段，基础信息已冻结；"
                    + "如需修改请先撤回或由管理员处理");
        }
        requireVersion(request.getVersion(), project);
        boolean draft = project.getStatus() == BidStatus.DRAFT;
        if (!draft) {
            validateRequired(request);
        }
        if (request.getName() != null) {
            if (request.getName().trim().isEmpty() && !draft) {
                throw BusinessException.invalid("项目名称不能为空");
            }
            project.setName(draft ? draftValue(request.getName(), "未命名草稿") : request.getName().trim());
        }
        if (request.getClientName() != null) {
            if (request.getClientName().trim().isEmpty() && !draft) {
                throw BusinessException.invalid("招标人不能为空");
            }
            project.setClientName(draft ? draftValue(request.getClientName(), "待补充") : request.getClientName().trim());
        }
        if (request.getCarrierName() != null) {
            project.setCarrierName(trim(request.getCarrierName()));
        }
        if (request.getIndustry() != null) {
            project.setIndustry(trim(request.getIndustry()));
        }
        if (request.getSource() != null) {
            project.setSource(trim(request.getSource()));
        }
        if (request.getProjectBrief() != null) {
            project.setProjectBrief(trim(request.getProjectBrief()));
        }
        if (request.getTenderText() != null) {
            project.setTenderText(trim(request.getTenderText()));
        }
        if (request.getDeadline() != null) {
            project.setDeadline(DateUtils.parseDateTime(request.getDeadline()));
        }
        if (request.getBudgetAmount() != null) {
            project.setBudgetAmount(request.getBudgetAmount());
        }
        if (request.getDepositAmount() != null) {
            project.setDepositAmount(request.getDepositAmount());
        }
        if (request.getProfile() != null) {
            project.setProfile(request.getProfile());
        }
        if ((actor.isAdmin() || actor.getRole() == UserRole.BID_MANAGER)
                && request.getOwnerId() != null && !request.getOwnerId().trim().isEmpty()) {
            User owner = resolveOwner(request.getOwnerId(), actor);
            project.setOwnerId(owner.getId());
            project.setOwnerName(owner.getDisplayName());
            project.setDepartment(owner.getDepartment());
        }
        if (actor.isAdmin() && request.getDepartment() != null && !request.getDepartment().trim().isEmpty()) {
            project.setDepartment(request.getDepartment().trim());
        }
        project.setVersion(project.getVersion() + 1);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.PROJECT_UPDATE, "更新项目基础信息");
        return detail(project.getId(), actor);
    }

    /**
     * 管理员与投标经理均可指定项目负责人；经理只能选择本部门负责人。
     */
    public User resolveOwner(String ownerId, CurrentUser actor) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> BusinessException.invalid("指定的项目负责人不存在"));
        assertOwnerAssignable(owner, actor);
        return owner;
    }

    public void assertOwnerAssignable(User owner, CurrentUser actor) {
        if (!owner.isEnabled()) {
            throw BusinessException.invalid("项目负责人账号已停用：" + owner.getUsername());
        }
        if (owner.getRole() != UserRole.ADMIN && owner.getRole() != UserRole.BID_MANAGER) {
            throw BusinessException.invalid("项目负责人只能选择管理员或投标经理");
        }
        if (!actor.isAdmin() && !owner.getDepartment().equals(actor.getDepartment())) {
            throw BusinessException.invalid("投标经理只能选择本部门的项目负责人");
        }
    }

    /**
     * 草稿允许信息不完整；进入资格预审前再统一校验必填项。
     */
    public void ensureReady(BidProject project) {
        if (project.getName() == null || project.getName().trim().isEmpty()
                || "未命名草稿".equals(project.getName())) {
            throw BusinessException.invalid("请先补全项目名称，再发起资格预审");
        }
        if (project.getClientName() == null || project.getClientName().trim().isEmpty()
                || "待补充".equals(project.getClientName())) {
            throw BusinessException.invalid("请先补全招标人/委托方，再发起资格预审");
        }
        if (project.getDeadline() == null) {
            throw BusinessException.invalid("请先填写投标截止时间，再发起资格预审");
        }
        if (project.getDeadline().isBefore(LocalDateTime.now())) {
            throw BusinessException.invalid("投标截止时间不能早于当前时间");
        }
        if (project.getBudgetAmount() == null || project.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.invalid("请先填写大于 0 的招标预算，再发起资格预审");
        }
    }

    public ProjectDtos.Detail detail(String id, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertView(actor, project);
        return toDetail(project, actor);
    }

    public ProjectDtos.Detail toDetail(BidProject project, CurrentUser actor) {
        int round = project.getReviews() == null ? 0
                : project.getReviews().stream().mapToInt(ReviewRecord::getRound).max().orElse(0);
        Map<String, Boolean> permissions = new LinkedHashMap<String, Boolean>();
        permissions.put("operate", permissionService.canOperate(actor, project));
        permissions.put("review", permissionService.canReview(actor, project));
        permissions.put("admin", actor.isAdmin());
        permissions.put("viewAudit", permissionService.canView(actor, project));

        List<ProjectDtos.AttachmentView> attachmentViews = new ArrayList<ProjectDtos.AttachmentView>();
        if (project.getAttachments() != null) {
            for (BidProject.Attachment attachment : project.getAttachments()) {
                attachmentViews.add(toAttachmentView(project, attachment));
            }
        }
        List<ProjectDtos.CommentView> commentViews = new ArrayList<ProjectDtos.CommentView>();
        if (project.getComments() != null) {
            for (com.sincit.bid.domain.ProjectComment comment : project.getComments()) {
                commentViews.add(ProjectDtos.CommentView.builder()
                        .id(comment.getId())
                        .authorId(comment.getAuthorId())
                        .authorName(comment.getAuthorName())
                        .avatarUrl(comment.getAuthorAvatarUrl())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .build());
            }
        }

        ProjectDtos.Detail detail = ProjectDtos.Detail.builder()
                .id(project.getId())
                .code(project.getCode())
                .name(project.getName())
                .clientName(project.getClientName())
                .carrierName(project.getCarrierName())
                .industry(project.getIndustry())
                .source(project.getSource())
                .status(project.getStatus().name())
                .statusLabel(project.getStatus().getLabel())
                .statusType(project.getStatus().getUiType())
                .stage(project.getStatus().getStage())
                .version(project.getVersion())
                .ownerId(project.getOwnerId())
                .ownerName(project.getOwnerName())
                .department(project.getDepartment())
                .createdBy(project.getCreatedBy())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .deadline(project.getDeadline())
                .daysToDeadline(daysToDeadline(project))
                .budgetAmount(project.getBudgetAmount())
                .bidAmount(project.getBidAmount())
                .depositAmount(project.getDepositAmount())
                .projectBrief(project.getProjectBrief())
                .tenderText(project.getTenderText())
                .profile(project.getProfile())
                .qualificationResult(project.getQualificationResult())
                .extraction(project.getExtraction())
                .matchReport(project.getMatchReport())
                .pricingPlan(project.getPricingPlan())
                .aiDraft(project.getAiDraft())
                .reviews(project.getReviews() == null ? new ArrayList<ReviewRecord>() : project.getReviews())
                .attachments(attachmentViews)
                .comments(commentViews)
                .submission(project.getSubmission())
                .archive(project.getArchive())
                .rejectionReason(project.getRejectionReason())
                .lastReviewOpinion(project.getLastReviewOpinion())
                .reviewRound(round)
                .following(actor != null && project.getWatcherIds() != null
                        && project.getWatcherIds().contains(actor.getId()))
                .attachmentCount(attachmentViews.size())
                .commentCount(commentViews.size())
                .risks(buildRisks(project))
                .permissions(permissions)
                .allowedActions(allowedActions(project, actor))
                .build();
        return detail;
    }

    public PageResult<ProjectDtos.Summary> list(String keyword, String status, String department,
                                                Boolean following, Boolean includeDraft, String view,
                                                int page, int size, CurrentUser actor) {
        boolean draftQuery = "DRAFT".equalsIgnoreCase(status == null ? "" : status.trim())
                || "todo".equalsIgnoreCase(view == null ? "" : view.trim());
        List<BidProject> visible = projectRepository.findAll().stream()
                .filter(p -> permissionService.canView(actor, p))
                .filter(p -> draftQuery || Boolean.TRUE.equals(includeDraft) || p.getStatus() != BidStatus.DRAFT)
                .filter(p -> filterKeyword(p, keyword))
                .filter(p -> filterStatus(p, status))
                .filter(p -> filterView(p, view, actor))
                .filter(p -> department == null || department.trim().isEmpty()
                        || department.equals(p.getDepartment()))
                .filter(p -> following == null || !following || isFollowing(p, actor))
                .sorted(Comparator.comparing(BidProject::getUpdatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
        List<ProjectDtos.Summary> summaries = visible.stream()
                .map(p -> toSummary(p, actor)).collect(Collectors.toList());
        return PageResult.of(summaries, page, size);
    }

    public List<BidProject> visibleProjects(CurrentUser actor) {
        return projectRepository.findAll().stream()
                .filter(p -> permissionService.canView(actor, p))
                .collect(Collectors.toList());
    }

    public ProjectDtos.Summary toSummary(BidProject project) {
        return toSummary(project, null);
    }

    public ProjectDtos.Summary toSummary(BidProject project, CurrentUser actor) {
        int round = project.getReviews() == null ? 0
                : project.getReviews().stream().mapToInt(ReviewRecord::getRound).max().orElse(0);
        return ProjectDtos.Summary.builder()
                .id(project.getId())
                .code(project.getCode())
                .name(project.getName())
                .clientName(project.getClientName())
                .carrierName(project.getCarrierName())
                .industry(project.getIndustry())
                .status(project.getStatus().name())
                .statusLabel(project.getStatus().getLabel())
                .statusType(project.getStatus().getUiType())
                .stage(project.getStatus().getStage())
                .ownerId(project.getOwnerId())
                .ownerName(project.getOwnerName())
                .department(project.getDepartment())
                .deadline(project.getDeadline())
                .budgetAmount(project.getBudgetAmount())
                .bidAmount(project.getBidAmount())
                .updatedAt(project.getUpdatedAt())
                .daysToDeadline(daysToDeadline(project))
                .riskLevel(riskLevel(project))
                .riskText(riskText(project))
                .reviewRound(round)
                .following(actor != null && isFollowing(project, actor))
                .attachmentCount(project.getAttachments() == null ? 0 : project.getAttachments().size())
                .commentCount(project.getComments() == null ? 0 : project.getComments().size())
                .build();
    }

    private ProjectDtos.AttachmentView toAttachmentView(BidProject project, BidProject.Attachment attachment) {
        return ProjectDtos.AttachmentView.builder()
                .id(attachment.getId())
                .filename(attachment.getFilename())
                .category(attachment.getCategory())
                .size(attachment.getSize())
                .contentType(attachment.getContentType())
                .uploadedAt(attachment.getUploadedAt())
                .uploadedById(attachment.getUploadedById())
                .uploadedBy(attachment.getUploadedBy())
                .downloadUrl("/api/projects/" + project.getId() + "/attachments/" + attachment.getId() + "/download")
                .build();
    }

    private boolean isFollowing(BidProject project, CurrentUser actor) {
        return actor != null && project.getWatcherIds() != null
                && project.getWatcherIds().contains(actor.getId());
    }

    public void requireVersion(Integer version, BidProject project) {
        if (version == null) {
            throw BusinessException.invalid("缺少数据版本号，请刷新页面后重试");
        }
        if (version != project.getVersion()) {
            throw BusinessException.conflict("数据已被他人更新（当前版本 " + project.getVersion()
                    + "），请刷新页面后重新操作");
        }
    }

    public long daysToDeadline(BidProject project) {
        if (project.getDeadline() == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), project.getDeadline());
    }

    private List<String> allowedActions(BidProject project, CurrentUser actor) {
        List<String> actions = new ArrayList<String>();
        boolean operate = permissionService.canOperate(actor, project);
        BidStatus status = project.getStatus();
        if (operate && status.isBaseEditable()) {
            actions.add("EDIT");
        }
        if (operate && (status == BidStatus.DRAFT || status == BidStatus.QUALIFICATION_REJECTED
                || status == BidStatus.QUALIFICATION_PASSED)) {
            actions.add("QUALIFICATION_CHECK");
        }
        if (operate && (status == BidStatus.QUALIFICATION_PASSED || status == BidStatus.REQUIREMENTS_EXTRACTED)) {
            actions.add("EXTRACT");
        }
        if (operate && (status == BidStatus.REQUIREMENTS_EXTRACTED || status == BidStatus.MATCHED)) {
            actions.add("MATCH_PRICE");
        }
        if (operate && (status == BidStatus.MATCHED || status == BidStatus.AI_DRAFTED
                || status == BidStatus.REVIEW_REJECTED)) {
            actions.add("AI_DRAFT");
        }
        if (operate && (status == BidStatus.AI_DRAFTED || status == BidStatus.REVIEW_REJECTED)) {
            actions.add("UPDATE_DRAFT");
            actions.add("SUBMIT_REVIEW");
        }
        if (status == BidStatus.REVIEW_PENDING && permissionService.canReview(actor, project)) {
            actions.add("REVIEW");
        }
        if (operate && status == BidStatus.APPROVED) {
            actions.add("SUBMIT");
        }
        if (operate && status == BidStatus.SUBMITTED) {
            actions.add("ARCHIVE");
        }
        return actions;
    }

    private List<String> buildRisks(BidProject project) {
        List<String> risks = new ArrayList<String>();
        BidStatus status = project.getStatus();
        if (status == BidStatus.QUALIFICATION_REJECTED && project.getRejectionReason() != null) {
            risks.add("资格预审自动驳回：" + project.getRejectionReason());
        }
        if (status == BidStatus.REVIEW_REJECTED && project.getLastReviewOpinion() != null) {
            risks.add("复核退回：" + project.getLastReviewOpinion());
        }
        long days = daysToDeadline(project);
        if (!status.isFinished() && days >= 0 && days <= 3) {
            risks.add("距投标截止仅 " + days + " 天，需立即完成复核、盖章与上传");
        } else if (!status.isFinished() && days < 0) {
            risks.add("投标截止时间已过，请确认是否放弃或申请延期");
        }
        if (project.getBudgetAmount() == null) {
            risks.add("尚未填写招标预算，无法完成报价测算");
        }
        if (project.getPricingPlan() != null && project.getPricingPlan().getGrossMarginRate() < 8) {
            risks.add("测算毛利率低于 8%，需复核成本口径与让利空间");
        }
        if (project.getQualificationResult() != null
                && "CONDITIONAL_PASS".equals(project.getQualificationResult().getConclusion())) {
            risks.add("资格预审为有条件通过，存在 " + project.getQualificationResult().getWarnings().size() + " 项预警");
        }
        if (status == BidStatus.REVIEW_PENDING && project.getUpdatedAt() != null
                && ChronoUnit.DAYS.between(project.getUpdatedAt(), LocalDateTime.now()) >= 2) {
            risks.add("复核已等待超过 2 天，建议提醒复核人处理");
        }
        return risks;
    }

    private String riskLevel(BidProject project) {
        if (project.getStatus().isRejected()) {
            return "HIGH";
        }
        long days = daysToDeadline(project);
        if (!project.getStatus().isFinished() && days >= 0 && days <= 3) {
            return "HIGH";
        }
        if (!project.getStatus().isFinished() && days >= 0 && days <= 7) {
            return "MEDIUM";
        }
        if (project.getStatus() == BidStatus.REVIEW_PENDING) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String riskText(BidProject project) {
        if (project.getStatus().isRejected()) {
            return "流程受阻";
        }
        long days = daysToDeadline(project);
        if (project.getStatus().isFinished()) {
            return "已完成";
        }
        if (days < 0) {
            return "已逾期";
        }
        if (days <= 3) {
            return "截止临近";
        }
        if (days <= 7) {
            return "需关注";
        }
        return "正常";
    }

    private boolean filterKeyword(BidProject project, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(project.getName(), value) || contains(project.getCode(), value)
                || contains(project.getClientName(), value) || contains(project.getCarrierName(), value)
                || contains(project.getIndustry(), value);
    }

    private boolean filterStatus(BidProject project, String status) {
        if (status == null || status.trim().isEmpty()) {
            return true;
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            return project.getStatus().isRejected();
        }
        if ("FINISHED".equalsIgnoreCase(status)) {
            return project.getStatus().isFinished();
        }
        return project.getStatus().name().equalsIgnoreCase(status.trim());
    }

    /**
     * 工作台卡片跳转用的轻量视图：推进中 / 待我处理 / 全部。
     */
    private boolean filterView(BidProject project, String view, CurrentUser actor) {
        if (view == null || view.trim().isEmpty() || "all".equalsIgnoreCase(view)) {
            return true;
        }
        if ("active".equalsIgnoreCase(view)) {
            return project.getStatus() != BidStatus.DRAFT
                    && !project.getStatus().isFinished() && !project.getStatus().isRejected();
        }
        if ("todo".equalsIgnoreCase(view)) {
            if (actor.isAdmin()) {
                return isManagerTodo(project.getStatus()) || project.getStatus() == BidStatus.REVIEW_PENDING;
            }
            if (actor.getRole() == UserRole.BID_MANAGER) {
                return isManagerTodo(project.getStatus());
            }
            if (actor.getRole() == UserRole.REVIEWER) {
                return project.getStatus() == BidStatus.REVIEW_PENDING;
            }
            return false;
        }
        return true;
    }

    private boolean isManagerTodo(BidStatus status) {
        return status != null && status != BidStatus.REVIEW_PENDING && status != BidStatus.ARCHIVED;
    }

    private boolean contains(String source, String value) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(value);
    }

    private void validateRequired(ProjectDtos.SaveRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw BusinessException.invalid("请填写项目名称");
        }
        if (request.getClientName() == null || request.getClientName().trim().isEmpty()) {
            throw BusinessException.invalid("请填写招标人/委托方");
        }
        if (request.getDeadline() == null || request.getDeadline().trim().isEmpty()) {
            throw BusinessException.invalid("请填写投标截止时间");
        }
        if (DateUtils.parseDateTime(request.getDeadline()).isBefore(LocalDateTime.now())) {
            throw BusinessException.invalid("投标截止时间不能早于当前时间");
        }
        if (request.getBudgetAmount() == null || request.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.invalid("请填写大于 0 的招标预算");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String draftValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String amountText(BigDecimal amount) {
        return amount == null ? "-" : amount.stripTrailingZeros().toPlainString();
    }
}
