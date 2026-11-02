package com.sincit.bid.service;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.BidStatus;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.web.dto.BatchDtos;
import com.sincit.bid.web.dto.RecycleBinDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目生命周期：软删除 -> 回收站（30 天）-> 恢复或彻底删除。
 * 草稿允许负责人自行删除；其他状态仅管理员可删除。
 */
@Service
public class ProjectLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ProjectLifecycleService.class);
    private static final int RETENTION_DAYS = 30;

    private final ProjectRepository projectRepository;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;

    public ProjectLifecycleService(ProjectRepository projectRepository, PermissionService permissionService,
                                   AuditService auditService, FileStorageService fileStorageService) {
        this.projectRepository = projectRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.fileStorageService = fileStorageService;
    }

    public void delete(String id, String reason, CurrentUser actor) {
        BidProject project = projectRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("项目不存在"));
        if (project.isDeleted()) {
            throw BusinessException.conflict("项目已在回收站中");
        }
        boolean isDraftOwner = project.getStatus() == BidStatus.DRAFT
                && actor.getId().equals(project.getOwnerId());
        if (!actor.isAdmin() && !isDraftOwner) {
            throw BusinessException.forbidden("仅管理员可删除项目；草稿可由负责人自行删除");
        }
        if (actor.isAdmin() && project.getStatus() == BidStatus.ARCHIVED) {
            // 归档项目允许删除，但保留删除原因，便于审计
            log.info("archived project moved to recycle bin, code={}", project.getCode());
        }
        project.setDeleted(true);
        project.setDeletedAt(LocalDateTime.now());
        project.setDeletedBy(actor.getDisplayName());
        project.setDeletedById(actor.getId());
        project.setDeleteReason(reason == null || reason.trim().isEmpty() ? "未填写删除原因" : reason.trim());
        project.setStatusBeforeDelete(project.getStatus());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.PROJECT_DELETE,
                "删除项目进入回收站：原状态 " + project.getStatusBeforeDelete().getLabel()
                        + "，原因：" + project.getDeleteReason());
    }

    public List<RecycleBinDtos.RecycleItem> list(CurrentUser actor) {
        permissionService.assertAdmin(actor);
        LocalDateTime now = LocalDateTime.now();
        return projectRepository.findAll().stream()
                .filter(BidProject::isDeleted)
                .sorted(Comparator.comparing(BidProject::getDeletedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(project -> toItem(project, now))
                .collect(Collectors.toList());
    }

    public BatchDtos.BatchResult batchDelete(List<String> ids, String reason, CurrentUser actor) {
        return runBatch(ids, id -> delete(id, reason, actor));
    }

    public BatchDtos.BatchResult batchRestore(List<String> ids, CurrentUser actor) {
        permissionService.assertAdmin(actor);
        return runBatch(ids, id -> restore(id, actor));
    }

    public BatchDtos.BatchResult batchPurge(List<String> ids, CurrentUser actor) {
        permissionService.assertAdmin(actor);
        return runBatch(ids, id -> purge(id, actor));
    }

    private BatchDtos.BatchResult runBatch(List<String> ids, BatchAction action) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.invalid("请选择要操作的项目");
        }
        if (ids.size() > 200) {
            throw BusinessException.invalid("单次最多操作 200 个项目");
        }
        int success = 0;
        List<BatchDtos.Failure> failures = new ArrayList<BatchDtos.Failure>();
        for (String id : new java.util.LinkedHashSet<String>(ids)) {
            try {
                action.run(id);
                success++;
            } catch (BusinessException e) {
                failures.add(BatchDtos.Failure.builder().id(id).message(e.getMessage()).build());
            } catch (Exception e) {
                failures.add(BatchDtos.Failure.builder().id(id)
                        .message("操作失败：" + (e.getMessage() == null ? "系统异常" : e.getMessage())).build());
            }
        }
        return BatchDtos.BatchResult.builder()
                .success(success)
                .failed(failures.size())
                .failures(failures)
                .build();
    }

    private interface BatchAction {
        void run(String id);
    }

    public RecycleBinDtos.RecycleItem restore(String id, CurrentUser actor) {
        permissionService.assertAdmin(actor);
        BidProject project = projectRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("项目不存在"));
        if (!project.isDeleted()) {
            throw BusinessException.conflict("项目不在回收站中");
        }
        project.setDeleted(false);
        project.setDeletedAt(null);
        project.setDeletedBy(null);
        project.setDeletedById(null);
        project.setDeleteReason(null);
        project.setStatus(project.getStatusBeforeDelete() == null ? BidStatus.DRAFT : project.getStatusBeforeDelete());
        project.setStatusBeforeDelete(null);
        project.setUpdatedAt(LocalDateTime.now());
        project.setVersion(project.getVersion() + 1);
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.PROJECT_RESTORE,
                "从回收站恢复，恢复状态：" + project.getStatus().getLabel());
        return toItem(project, LocalDateTime.now());
    }

    public void purge(String id, CurrentUser actor) {
        permissionService.assertAdmin(actor);
        BidProject project = projectRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("项目不存在"));
        if (!project.isDeleted()) {
            throw BusinessException.conflict("只能彻底删除回收站中的项目");
        }
        hardDelete(project, actor);
    }

    /**
     * 定时清理超过 30 天的回收站项目。系统动作也写审计。
     */
    @Scheduled(fixedDelay = 21600000L, initialDelay = 120000L)
    public void purgeExpired() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<BidProject> expired = projectRepository.findAll().stream()
                .filter(BidProject::isDeleted)
                .filter(p -> p.getDeletedAt() != null && p.getDeletedAt().isBefore(threshold))
                .collect(Collectors.toList());
        for (BidProject project : expired) {
            hardDelete(project, null);
        }
        if (!expired.isEmpty()) {
            log.info("recycle bin auto purge finished, count={}", expired.size());
        }
    }

    private void hardDelete(BidProject project, CurrentUser actor) {
        if (project.getAttachments() != null) {
            for (BidProject.Attachment attachment : new ArrayList<BidProject.Attachment>(project.getAttachments())) {
                fileStorageService.deleteAttachment(project.getId(), attachment.getStorageName());
            }
        }
        projectRepository.deleteById(project.getId());
        auditService.record(actor, project, AuditAction.PROJECT_PURGE,
                "彻底删除项目：" + project.getName() + "（" + project.getCode() + "），附件已清理");
    }

    private RecycleBinDtos.RecycleItem toItem(BidProject project, LocalDateTime now) {
        long passed = project.getDeletedAt() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(project.getDeletedAt(), now));
        long remaining = Math.max(0, RETENTION_DAYS - passed);
        return RecycleBinDtos.RecycleItem.builder()
                .id(project.getId())
                .code(project.getCode())
                .name(project.getName())
                .clientName(project.getClientName())
                .ownerName(project.getOwnerName())
                .department(project.getDepartment())
                .statusLabel(project.getStatusBeforeDelete() != null
                        ? project.getStatusBeforeDelete().getLabel()
                        : project.getStatus() == null ? "未知" : project.getStatus().getLabel())
                .budgetAmount(project.getBudgetAmount())
                .deletedAt(project.getDeletedAt())
                .deletedBy(project.getDeletedBy())
                .deleteReason(project.getDeleteReason())
                .remainingDays(remaining)
                .build();
    }
}
