package com.sincit.bid.service;

import com.sincit.bid.auth.AuthService;
import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PasswordHasher;
import com.sincit.bid.auth.PasswordPolicy;
import com.sincit.bid.auth.TokenService;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.AuditLog;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.Notification;
import com.sincit.bid.domain.User;
import com.sincit.bid.persistence.AuditRepository;
import com.sincit.bid.persistence.NotificationRepository;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.web.dto.AuthDtos;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人中心：资料、头像、密码、通知偏好、我的统计与动态。
 */
@Service
public class ProfileService {

    private static final Set<String> PREF_KEYS = new HashSet<String>(Arrays.asList(
            "reviewTask", "reviewResult", "comment", "deadline", "status"));

    private final UserRepository userRepository;
    private final AuthService authService;
    private final TokenService tokenService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final AuditRepository auditRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;

    public ProfileService(UserRepository userRepository, AuthService authService, TokenService tokenService,
                          FileStorageService fileStorageService, AuditService auditService,
                          AuditRepository auditRepository, ProjectRepository projectRepository,
                          NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.tokenService = tokenService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.auditRepository = auditRepository;
        this.projectRepository = projectRepository;
        this.notificationRepository = notificationRepository;
    }

    public AuthDtos.UserView view(CurrentUser actor) {
        return authService.toView(get(actor));
    }

    public AuthDtos.UserView updateProfile(CurrentUser actor, AuthDtos.ProfileUpdateRequest request) {
        User user = get(actor);
        String displayName = trim(request.getDisplayName());
        if (displayName.isEmpty()) {
            throw BusinessException.invalid("昵称不能为空");
        }
        if (displayName.length() < 2 || displayName.length() > 20) {
            throw BusinessException.invalid("昵称长度需为 2-20 个字符");
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()
                && !request.getPhone().trim().matches("^1[3-9]\\d{9}$")) {
            throw BusinessException.invalid("手机号格式不正确");
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()
                && !request.getEmail().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw BusinessException.invalid("邮箱格式不正确");
        }
        if (request.getTitle() != null && request.getTitle().length() > 30) {
            throw BusinessException.invalid("职位不能超过 30 个字符");
        }
        if (request.getBio() != null && request.getBio().length() > 200) {
            throw BusinessException.invalid("个人简介不能超过 200 个字符");
        }
        user.setDisplayName(displayName);
        user.setPhone(trimNullable(request.getPhone()));
        user.setEmail(trimNullable(request.getEmail()));
        user.setTitle(trimNullable(request.getTitle()));
        user.setBio(trimNullable(request.getBio()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.record(actor, null, AuditAction.PROFILE_UPDATE, "更新个人资料：昵称 " + displayName);
        return authService.toView(user);
    }

    public AuthDtos.UserView updateAvatar(CurrentUser actor, MultipartFile file) {
        User user = get(actor);
        String filename = fileStorageService.saveAvatar(user.getId(), file);
        fileStorageService.deleteAvatar(user.getAvatar());
        user.setAvatar(filename);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.record(actor, null, AuditAction.AVATAR_UPDATE, "更新个人头像");
        return authService.toView(user);
    }

    public AuthDtos.UserView removeAvatar(CurrentUser actor) {
        User user = get(actor);
        fileStorageService.deleteAvatar(user.getAvatar());
        user.setAvatar(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.record(actor, null, AuditAction.AVATAR_UPDATE, "恢复默认头像");
        return authService.toView(user);
    }

    public AuthDtos.PasswordChangeResponse changePassword(CurrentUser actor, AuthDtos.PasswordChangeRequest request) {
        User user = get(actor);
        if (request.getOldPassword() == null
                || !PasswordHasher.matches(request.getOldPassword(), user.getSalt(), user.getPasswordHash())) {
            throw BusinessException.invalid("原密码不正确");
        }
        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw BusinessException.invalid("两次输入的新密码不一致");
        }
        PasswordPolicy.validate(request.getNewPassword());
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw BusinessException.invalid("新密码不能与原密码相同");
        }
        String salt = PasswordHasher.newSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordHasher.hash(request.getNewPassword(), salt));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.record(actor, null, AuditAction.PASSWORD_CHANGE, "修改登录密码，旧令牌已失效");
        return AuthDtos.PasswordChangeResponse.builder()
                .token(tokenService.issue(user))
                .expireAt(tokenService.expireAtText())
                .build();
    }

    public Map<String, Boolean> updatePreferences(CurrentUser actor, Map<String, Boolean> preferences) {
        User user = get(actor);
        Map<String, Boolean> merged = user.getNotificationPrefs() == null
                ? new LinkedHashMap<String, Boolean>() : new LinkedHashMap<String, Boolean>(user.getNotificationPrefs());
        if (preferences != null) {
            for (Map.Entry<String, Boolean> entry : preferences.entrySet()) {
                if (PREF_KEYS.contains(entry.getKey()) && entry.getValue() != null) {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
        }
        user.setNotificationPrefs(merged);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.record(actor, null, AuditAction.PREFERENCES_UPDATE, "更新通知偏好");
        return merged;
    }

    public Map<String, Object> summary(CurrentUser actor) {
        List<BidProject> mine = projectRepository.findAll().stream()
                .filter(p -> actor.getId().equals(p.getOwnerId()))
                .collect(Collectors.toList());
        long active = mine.stream().filter(p -> !p.getStatus().isFinished() && !p.getStatus().isRejected()).count();
        long completed = mine.stream().filter(p -> p.getStatus().isFinished()).count();
        long pending = mine.stream().filter(p -> p.getStatus() == com.sincit.bid.domain.BidStatus.REVIEW_PENDING).count();
        long following = projectRepository.findAll().stream()
                .filter(p -> p.getWatcherIds() != null && p.getWatcherIds().contains(actor.getId())).count();
        BigDecimal budget = mine.stream().map(BidProject::getBudgetAmount)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("myProjects", mine.size());
        data.put("activeProjects", active);
        data.put("completedProjects", completed);
        data.put("pendingReview", pending);
        data.put("followingProjects", following);
        data.put("unreadNotifications", notificationRepository.countUnread(actor.getId()));
        data.put("totalBudget", budget);
        return data;
    }

    public List<AuditLog> activity(CurrentUser actor) {
        List<AuditLog> logs = auditRepository.findAll().stream()
                .filter(log -> actor.getId().equals(log.getActorId()))
                .sorted(Comparator.comparing(AuditLog::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(20)
                .collect(Collectors.toList());
        return new ArrayList<AuditLog>(logs);
    }

    private User get(CurrentUser actor) {
        return userRepository.findById(actor.getId())
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在"));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
