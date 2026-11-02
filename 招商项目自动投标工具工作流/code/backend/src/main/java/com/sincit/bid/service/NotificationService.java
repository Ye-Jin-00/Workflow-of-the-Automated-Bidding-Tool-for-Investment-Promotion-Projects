package com.sincit.bid.service;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.BidStatus;
import com.sincit.bid.domain.Notification;
import com.sincit.bid.domain.Notification.NotificationType;
import com.sincit.bid.domain.User;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.persistence.NotificationRepository;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.web.dto.PageResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 站内消息：复核任务、复核结果、状态变更、评论、附件与截止提醒。
 * 通知偏好由用户个人中心控制；dedupeKey 防止定时任务重复提醒。
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
                               ProjectRepository projectRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    public void notifyReviewTask(BidProject project, CurrentUser actor) {
        Set<String> recipients = new LinkedHashSet<String>();
        for (User user : userRepository.findAll()) {
            if (!user.isEnabled() || user.getId().equals(project.getOwnerId())) {
                continue;
            }
            if (user.getRole() == UserRole.REVIEWER || user.getRole() == UserRole.ADMIN) {
                recipients.add(user.getId());
            }
        }
        String dedupe = "review:" + project.getId() + ":" + project.getVersion();
        notifyUsers(recipients, NotificationType.REVIEW_TASK, "新的待复核项目",
                "《" + project.getName() + "》已提交复核，请在截止前完成审核。",
                project, dedupe, "reviewTask");
    }

    public void markReviewTaskRead(BidProject project) {
        notificationRepository.markReviewTaskRead(project.getId());
    }

    public void notifyReviewResult(BidProject project, CurrentUser actor, boolean approved, String opinion) {
        Set<String> recipients = new LinkedHashSet<String>();
        recipients.add(project.getOwnerId());
        String title = approved ? "复核通过" : "复核退回";
        String content = approved
                ? "《" + project.getName() + "》已通过复核，可提交投标。"
                : "《" + project.getName() + "》被退回：" + (opinion == null ? "请查看修改意见" : opinion);
        notifyUsers(recipients, NotificationType.REVIEW_RESULT, title, content,
                project, "review-result:" + project.getId() + ":" + project.getVersion(), "reviewResult");
    }

    public void notifyStatus(BidProject project, CurrentUser actor) {
        Set<String> recipients = recipientsOfOwnerAndWatchers(project);
        recipients.remove(actor == null ? null : actor.getId());
        notifyUsers(recipients, NotificationType.STATUS, "项目状态更新",
                "《" + project.getName() + "》当前状态：" + project.getStatus().getLabel(),
                project, "status:" + project.getId() + ":" + project.getStatus().name(), "status");
    }

    public void notifyComment(BidProject project, CurrentUser actor) {
        Set<String> recipients = recipientsOfOwnerAndWatchers(project);
        recipients.remove(actor.getId());
        notifyUsers(recipients, NotificationType.COMMENT, "项目有新评论",
                actor.getDisplayName() + " 评论了《" + project.getName() + "》。",
                project, null, "comment");
    }

    public void notifyAttachment(BidProject project, CurrentUser actor, String filename) {
        Set<String> recipients = recipientsOfOwnerAndWatchers(project);
        recipients.remove(actor.getId());
        notifyUsers(recipients, NotificationType.ATTACHMENT, "项目新增文件",
                actor.getDisplayName() + " 上传了 " + filename + "。",
                project, null, "status");
    }

    public void notifyDeadline(BidProject project, long days) {
        Set<String> recipients = recipientsOfOwnerAndWatchers(project);
        String content = days < 0
                ? "《" + project.getName() + "》已过投标截止时间，请尽快确认是否放弃或申请延期。"
                : "《" + project.getName() + "》距投标截止仅 " + days + " 天，请尽快完成复核与提交。";
        String dedupe = "deadline:" + project.getId() + ":" + LocalDate.now();
        notifyUsers(recipients, NotificationType.DEADLINE,
                "投标截止提醒", content, project, dedupe, "deadline");
    }

    public void notifySystem(String userId, String title, String content) {
        notifyUsers(java.util.Collections.singleton(userId), NotificationType.SYSTEM,
                title, content, null, null, null);
    }

    public PageResult<Notification> list(CurrentUser actor, Boolean unreadOnly, String type, int page, int size) {
        List<Notification> items = notificationRepository.findByUserId(actor.getId()).stream()
                .filter(n -> unreadOnly == null || !unreadOnly || !n.isRead())
                .filter(n -> type == null || type.trim().isEmpty() || n.getType().name().equalsIgnoreCase(type))
                .sorted(Comparator.comparing(Notification::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
        return PageResult.of(items, page, size);
    }

    public int unreadCount(CurrentUser actor) {
        return notificationRepository.countUnread(actor.getId());
    }

    public void markRead(String id, CurrentUser actor) {
        if (!notificationRepository.markRead(id, actor.getId())) {
            throw BusinessException.notFound("消息不存在");
        }
    }

    public int markAllRead(CurrentUser actor) {
        return notificationRepository.markAllRead(actor.getId());
    }

    public void delete(String id, CurrentUser actor) {
        if (!notificationRepository.delete(id, actor.getId())) {
            throw BusinessException.notFound("消息不存在");
        }
    }

    /**
     * 首次启动时给演示账号几条真实感消息，避免“消息中心空空如也”。
     */
    public void seedDemo() {
        List<Notification> items = new ArrayList<Notification>();
        if (notificationRepository.count() == 0) {
            for (BidProject project : projectRepository.findAll()) {
                if (project.getStatus() == BidStatus.REVIEW_PENDING) {
                    items.add(build("U003", NotificationType.REVIEW_TASK, "新的待复核项目",
                            "《" + project.getName() + "》已提交复核，请在截止前完成审核。", project, "seed-review"));
                    items.add(build("U001", NotificationType.REVIEW_TASK, "待复核任务提醒",
                            "复核中心有 1 个项目等待处理：" + project.getCode(), project, "seed-review-admin"));
                }
                if (project.getStatus() == BidStatus.REVIEW_REJECTED && project.getOwnerId() != null) {
                    items.add(build(project.getOwnerId(), NotificationType.REVIEW_RESULT, "复核退回",
                            "《" + project.getName() + "》被退回：" + project.getLastReviewOpinion(),
                            project, "seed-return"));
                }
                if (project.getStatus() == BidStatus.ARCHIVED && project.getOwnerId() != null) {
                    String archiveNo = project.getArchive() == null ? project.getCode() : project.getArchive().getArchiveNo();
                    items.add(build(project.getOwnerId(), NotificationType.STATUS, "项目已归档",
                            "《" + project.getName() + "》已完成归档，档号 " + archiveNo,
                            project, "seed-archive"));
                }
            }
        }
        if (!notificationRepository.existsDedupe("U001", "seed-welcome-admin")) {
            items.add(build("U001", NotificationType.SYSTEM, "欢迎使用招商智投",
                    "个人中心已上线：可修改昵称、头像、密码与通知偏好。", null, "seed-welcome-admin"));
        }
        if (!notificationRepository.existsDedupe("U004", "seed-welcome-viewer")) {
            items.add(build("U004", NotificationType.SYSTEM, "只读观察员账号已开通",
                    "您可以查看已通过、已提交与已归档项目，但不能编辑或推进流程。", null, "seed-welcome-viewer"));
        }
        notificationRepository.saveAll(items);
        for (BidProject project : projectRepository.findAll()) {
            if (project.getStatus() != BidStatus.REVIEW_PENDING) {
                markReviewTaskRead(project);
            }
        }
    }

    private Notification build(String userId, NotificationType type, String title, String content,
                               BidProject project, String dedupeKey) {
        return Notification.builder()
                .id("N" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .projectId(project == null ? null : project.getId())
                .projectCode(project == null ? null : project.getCode())
                .link(project == null ? null : "/projects/" + project.getId())
                .read(false)
                .dedupeKey(dedupeKey)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void notifyUsers(Set<String> userIds, NotificationType type, String title, String content,
                             BidProject project, String dedupeKey, String prefKey) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<Notification> items = new ArrayList<Notification>();
        for (String userId : userIds) {
            if (userId == null) {
                continue;
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isEnabled()) {
                continue;
            }
            if (prefKey != null && !user.isNotifyEnabled(prefKey)) {
                continue;
            }
            String userDedupe = dedupeKey == null ? null : dedupeKey + ":" + userId;
            if (notificationRepository.existsDedupe(userId, userDedupe)) {
                continue;
            }
            items.add(build(userId, type, title, content, project, userDedupe));
        }
        notificationRepository.saveAll(items);
    }

    private Set<String> recipientsOfOwnerAndWatchers(BidProject project) {
        Set<String> recipients = new LinkedHashSet<String>();
        if (project.getOwnerId() != null) {
            recipients.add(project.getOwnerId());
        }
        if (project.getWatcherIds() != null) {
            recipients.addAll(project.getWatcherIds());
        }
        return recipients;
    }
}
