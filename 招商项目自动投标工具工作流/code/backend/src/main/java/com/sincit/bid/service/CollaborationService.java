package com.sincit.bid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.BidStatus;
import com.sincit.bid.domain.ProjectComment;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.User;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 项目协作：附件、评论、关注与复制。
 * 附件与评论不推进业务状态，不增加业务 version，避免前端因协作操作产生版本冲突。
 */
@Service
public class CollaborationService {

    private static final List<String> CATEGORIES = Arrays.asList(
            "招标文件", "资格材料", "标书文件", "回执凭证", "其他");

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ProjectService projectService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public CollaborationService(ProjectRepository projectRepository, UserRepository userRepository,
                                PermissionService permissionService, ProjectService projectService,
                                FileStorageService fileStorageService, AuditService auditService,
                                NotificationService notificationService, ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.projectService = projectService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    public ProjectDtos.Detail uploadAttachment(String id, MultipartFile file, String category,
                                               CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        if (project.getStatus() == BidStatus.ARCHIVED) {
            throw BusinessException.conflict("项目已归档，不能再上传文件");
        }
        String safeCategory = CATEGORIES.contains(category) ? category : "其他";
        FileStorageService.StoredFile stored = fileStorageService.saveAttachment(project.getId(), file);
        BidProject.Attachment attachment = BidProject.Attachment.builder()
                .id("F" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .filename(stored.getOriginalName())
                .category(safeCategory)
                .size(stored.getSize())
                .contentType(stored.getContentType())
                .storageName(stored.getStorageName())
                .uploadedById(actor.getId())
                .uploadedBy(actor.getDisplayName())
                .uploadedAt(LocalDateTime.now())
                .build();
        if (project.getAttachments() == null) {
            project.setAttachments(new ArrayList<BidProject.Attachment>());
        }
        project.getAttachments().add(attachment);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.ATTACHMENT_UPLOAD,
                "上传附件：" + stored.getOriginalName() + "（" + safeCategory + "）");
        notificationService.notifyAttachment(project, actor, stored.getOriginalName());
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail deleteAttachment(String id, String attachmentId, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertOperate(actor, project);
        if (project.getStatus() == BidStatus.ARCHIVED) {
            throw BusinessException.conflict("项目已归档，不能删除文件");
        }
        BidProject.Attachment target = null;
        if (project.getAttachments() != null) {
            for (BidProject.Attachment attachment : project.getAttachments()) {
                if (attachment.getId().equals(attachmentId)) {
                    target = attachment;
                    break;
                }
            }
        }
        if (target == null) {
            throw BusinessException.notFound("附件不存在");
        }
        project.getAttachments().remove(target);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        fileStorageService.deleteAttachment(project.getId(), target.getStorageName());
        auditService.record(actor, project, AuditAction.ATTACHMENT_DELETE, "删除附件：" + target.getFilename());
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail addComment(String id, String content, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertView(actor, project);
        if (actor.getRole() == UserRole.VIEWER) {
            throw BusinessException.forbidden("只读观察员不能发表评论");
        }
        if (project.getStatus() == BidStatus.ARCHIVED) {
            throw BusinessException.conflict("项目已归档，不能再发表评论");
        }
        String text = content == null ? "" : content.trim();
        if (text.length() < 1) {
            throw BusinessException.invalid("评论内容不能为空");
        }
        if (text.length() > 1000) {
            throw BusinessException.invalid("评论内容不能超过 1000 个字符");
        }
        User user = userRepository.findById(actor.getId())
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在"));
        String avatarUrl = user.getAvatar() == null ? null
                : "/api/avatars/" + user.getId() + "?v=" + user.getAvatar();
        ProjectComment comment = ProjectComment.builder()
                .id("C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10))
                .authorId(actor.getId())
                .authorName(actor.getDisplayName())
                .authorAvatarUrl(avatarUrl)
                .content(text)
                .createdAt(LocalDateTime.now())
                .build();
        if (project.getComments() == null) {
            project.setComments(new ArrayList<ProjectComment>());
        }
        project.getComments().add(comment);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.COMMENT_ADD, "发表评论：" + brief(text));
        notificationService.notifyComment(project, actor);
        return projectService.toDetail(project, actor);
    }

    public ProjectDtos.Detail deleteComment(String id, String commentId, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertView(actor, project);
        ProjectComment target = null;
        if (project.getComments() != null) {
            for (ProjectComment comment : project.getComments()) {
                if (comment.getId().equals(commentId)) {
                    target = comment;
                    break;
                }
            }
        }
        if (target == null) {
            throw BusinessException.notFound("评论不存在");
        }
        if (!actor.isAdmin() && !actor.getId().equals(target.getAuthorId())) {
            throw BusinessException.forbidden("只能删除自己的评论");
        }
        project.getComments().remove(target);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        auditService.record(actor, project, AuditAction.COMMENT_DELETE, "删除评论：" + brief(target.getContent()));
        return projectService.toDetail(project, actor);
    }

    public Map<String, Object> toggleFollow(String id, CurrentUser actor) {
        BidProject project = projectRepository.get(id);
        permissionService.assertView(actor, project);
        if (project.getWatcherIds() == null) {
            project.setWatcherIds(new ArrayList<String>());
        }
        boolean following;
        if (project.getWatcherIds().contains(actor.getId())) {
            project.getWatcherIds().remove(actor.getId());
            following = false;
            auditService.record(actor, project, AuditAction.UNFOLLOW, "取消关注项目");
        } else {
            project.getWatcherIds().add(actor.getId());
            following = true;
            auditService.record(actor, project, AuditAction.FOLLOW, "关注项目，接收状态与截止提醒");
        }
        projectRepository.save(project);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("following", following);
        data.put("watcherCount", project.getWatcherIds().size());
        return data;
    }

    /**
     * 复制项目基础信息与资格画像，生成新的草稿，便于复用同类投标。
     */
    public ProjectDtos.Detail duplicate(String id, CurrentUser actor) {
        BidProject source = projectRepository.get(id);
        permissionService.assertOperate(actor, source);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = source.getDeadline();
        if (deadline == null || deadline.isBefore(now)) {
            deadline = now.plusDays(14).withHour(17).withMinute(0).withSecond(0).withNano(0);
        }
        QualificationProfile profile = source.getProfile() == null ? null
                : objectMapper.convertValue(source.getProfile(), QualificationProfile.class);
        BidProject copy = BidProject.builder()
                .id("P" + System.currentTimeMillis())
                .code(projectRepository.nextCode())
                .name(source.getName() + "（复制）")
                .clientName(source.getClientName())
                .carrierName(source.getCarrierName())
                .industry(source.getIndustry())
                .source(source.getSource())
                .ownerId(actor.getId())
                .ownerName(actor.getDisplayName())
                .department(actor.getDepartment())
                .status(BidStatus.DRAFT)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor.getDisplayName())
                .deadline(deadline)
                .budgetAmount(source.getBudgetAmount())
                .depositAmount(source.getDepositAmount())
                .projectBrief(source.getProjectBrief())
                .tenderText(source.getTenderText())
                .profile(profile)
                .build();
        projectRepository.save(copy);
        auditService.record(actor, copy, AuditAction.PROJECT_DUPLICATE,
                "从 " + source.getCode() + " 复制生成草稿");
        return projectService.toDetail(copy, actor);
    }

    private String brief(String text) {
        return text.length() > 60 ? text.substring(0, 60) + "..." : text;
    }
}
