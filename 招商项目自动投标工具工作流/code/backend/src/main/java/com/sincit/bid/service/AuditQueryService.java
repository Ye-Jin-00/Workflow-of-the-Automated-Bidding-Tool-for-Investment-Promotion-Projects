package com.sincit.bid.service;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.AuditLog;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.persistence.AuditRepository;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.web.dto.PageResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 审计查询。观察员不可访问；投标经理仅可见自己有权限的项目日志。
 */
@Service
public class AuditQueryService {

    private final AuditRepository auditRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final PermissionService permissionService;

    public AuditQueryService(AuditRepository auditRepository, ProjectRepository projectRepository,
                             ProjectService projectService, PermissionService permissionService) {
        this.auditRepository = auditRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.permissionService = permissionService;
    }

    public PageResult<AuditLog> query(String projectId, String action, String keyword, int page, int size,
                                      CurrentUser actor) {
        if (actor.getRole() == UserRole.VIEWER) {
            throw BusinessException.forbidden("只读观察员无权查看审计日志");
        }
        if (projectId != null && !projectId.trim().isEmpty()) {
            BidProject project = projectRepository.get(projectId);
            permissionService.assertView(actor, project);
        }
        Set<String> visibleProjectIds = projectService.visibleProjects(actor).stream()
                .map(BidProject::getId).collect(Collectors.toSet());

        List<AuditLog> logs = auditRepository.findAll().stream()
                .filter(log -> projectId == null || projectId.trim().isEmpty() || projectId.equals(log.getProjectId()))
                .filter(log -> log.getProjectId() == null || visibleProjectIds.contains(log.getProjectId())
                        || actor.isAdmin())
                .filter(log -> action == null || action.trim().isEmpty() || action.equalsIgnoreCase(log.getAction()))
                .filter(log -> matchesKeyword(log, keyword))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
        return PageResult.of(logs, page, size);
    }

    public List<AuditLog> export(String projectId, String action, String keyword, CurrentUser actor) {
        return query(projectId, action, keyword, 1, 10000, actor).getItems();
    }

    public List<AuditLog> timeline(String projectId, CurrentUser actor) {
        BidProject project = projectRepository.get(projectId);
        permissionService.assertView(actor, project);
        return auditRepository.findByProject(projectId).stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(AuditLog log, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(log.getActorName(), value) || contains(log.getDetail(), value)
                || contains(log.getProjectCode(), value) || contains(log.getActionLabel(), value)
                || contains(log.getAction(), value);
    }

    private boolean contains(String source, String value) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(value);
    }
}
