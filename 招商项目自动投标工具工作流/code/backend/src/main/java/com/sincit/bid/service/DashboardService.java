package com.sincit.bid.service;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.BidStatus;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.web.dto.DashboardDtos;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作台聚合。数字全部按当前用户的数据权限计算，不做全量泄漏。
 */
@Service
public class DashboardService {

    private static final String[] STAGE_LABELS = {"", "项目录入", "资格预审", "要素抽取", "匹配测算", "材料生成", "人工复核", "提交归档"};

    private final ProjectService projectService;
    private final PermissionService permissionService;

    public DashboardService(ProjectService projectService, PermissionService permissionService) {
        this.projectService = projectService;
        this.permissionService = permissionService;
    }

    public DashboardDtos.Overview overview(CurrentUser actor) {
        List<BidProject> projects = projectService.visibleProjects(actor);
        long drafts = projects.stream().filter(p -> p.getStatus() == BidStatus.DRAFT).count();
        List<BidProject> formalProjects = projects.stream()
                .filter(p -> p.getStatus() != BidStatus.DRAFT).collect(Collectors.toList());
        long active = formalProjects.stream().filter(p -> !p.getStatus().isFinished() && !p.getStatus().isRejected()).count();
        long pendingReview = formalProjects.stream().filter(p -> p.getStatus() == BidStatus.REVIEW_PENDING).count();
        long rejected = formalProjects.stream().filter(p -> p.getStatus().isRejected()).count();
        long finished = formalProjects.stream().filter(p -> p.getStatus().isFinished()).count();
        BigDecimal totalBudget = formalProjects.stream().map(BidProject::getBudgetAmount)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBid = formalProjects.stream().map(BidProject::getBidAmount)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardDtos.TodoItem> todos = buildTodos(projects, actor);
        List<ProjectDtos.Summary> recent = projects.stream()
                .sorted(Comparator.comparing(BidProject::getUpdatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .map(p -> projectService.toSummary(p, actor))
                .collect(Collectors.toList());

        List<DashboardDtos.RiskItem> risks = new ArrayList<DashboardDtos.RiskItem>();
        for (BidProject project : projects) {
            String level = projectService.toSummary(project).getRiskLevel();
            ProjectDtos.Detail detail = projectService.toDetail(project, actor);
            if (!detail.getRisks().isEmpty() && (!"LOW".equals(level) || detail.getRisks().size() > 1)) {
                risks.add(DashboardDtos.RiskItem.builder()
                        .projectId(project.getId())
                        .code(project.getCode())
                        .name(project.getName())
                        .level(level)
                        .text(detail.getRisks().get(0))
                        .build());
            }
        }
        risks = risks.stream().limit(6).collect(Collectors.toList());

        int[] counts = new int[8];
        for (BidProject project : projects) {
            counts[project.getStatus().getStage()]++;
        }
        List<DashboardDtos.StageItem> stages = new ArrayList<DashboardDtos.StageItem>();
        for (int i = 1; i <= 7; i++) {
            stages.add(DashboardDtos.StageItem.builder().stage(i).label(STAGE_LABELS[i]).count(counts[i]).build());
        }

        DashboardDtos.Stats stats = DashboardDtos.Stats.builder()
                .total(formalProjects.size())
                .drafts(drafts)
                .active(active)
                .pendingReview(pendingReview)
                .rejected(rejected)
                .finished(finished)
                .todo(todos.size())
                .totalBudget(totalBudget)
                .totalBid(totalBid)
                .build();

        return DashboardDtos.Overview.builder()
                .stats(stats)
                .stages(stages)
                .todos(todos)
                .recentProjects(recent)
                .risks(risks)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private List<DashboardDtos.TodoItem> buildTodos(List<BidProject> projects, CurrentUser actor) {
        List<DashboardDtos.TodoItem> todos = new ArrayList<DashboardDtos.TodoItem>();
        for (BidProject project : projects) {
            String[] action = nextAction(project, actor);
            if (action == null) {
                continue;
            }
            todos.add(DashboardDtos.TodoItem.builder()
                    .projectId(project.getId())
                    .code(project.getCode())
                    .name(project.getName())
                    .statusLabel(project.getStatus().getLabel())
                    .action(action[0])
                    .actionLabel(action[1])
                    .deadline(project.getDeadline())
                    .daysToDeadline(projectService.daysToDeadline(project))
                    .build());
        }
        todos.sort(Comparator.comparing(DashboardDtos.TodoItem::getDeadline,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return todos.stream().limit(10).collect(Collectors.toList());
    }

    private String[] nextAction(BidProject project, CurrentUser actor) {
        BidStatus status = project.getStatus();
        if (status == BidStatus.REVIEW_PENDING) {
            return permissionService.canReview(actor, project) ? new String[]{"REVIEW", "完成人工复核"} : null;
        }
        if (actor.getRole() == UserRole.VIEWER || !permissionService.canOperate(actor, project)) {
            return null;
        }
        switch (status) {
            case DRAFT:
                return new String[]{"QUALIFICATION_CHECK", "完成资格预审"};
            case QUALIFICATION_REJECTED:
                return new String[]{"QUALIFICATION_CHECK", "修正资料后重新预审"};
            case QUALIFICATION_PASSED:
                return new String[]{"EXTRACT", "抽取招标文件要素"};
            case REQUIREMENTS_EXTRACTED:
                return new String[]{"MATCH_PRICE", "完成匹配与报价测算"};
            case MATCHED:
                return new String[]{"AI_DRAFT", "生成标书初稿"};
            case AI_DRAFTED:
                return new String[]{"SUBMIT_REVIEW", "提交人工复核"};
            case REVIEW_REJECTED:
                return new String[]{"SUBMIT_REVIEW", "修改后重新提交复核"};
            case APPROVED:
                return new String[]{"SUBMIT", "提交投标"};
            case SUBMITTED:
                return new String[]{"ARCHIVE", "完成项目归档"};
            default:
                return null;
        }
    }
}
