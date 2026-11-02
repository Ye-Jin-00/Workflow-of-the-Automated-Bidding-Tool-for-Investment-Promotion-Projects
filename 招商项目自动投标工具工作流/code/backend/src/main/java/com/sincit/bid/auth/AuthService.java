package com.sincit.bid.auth;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.User;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.service.AuditService;
import com.sincit.bid.web.dto.AuthDtos;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录认证：连续失败 5 次锁定 5 分钟；登录成功更新最近登录时间并写审计。
 */
@Service
public class AuthService {

    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 5 * 60 * 1000L;

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<String, Attempt>();

    public AuthService(UserRepository userRepository, TokenService tokenService, AuditService auditService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.auditService = auditService;
    }

    public AuthDtos.LoginResponse login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw BusinessException.invalid("请输入用户名和密码");
        }
        String key = username.trim().toLowerCase();
        checkLocked(key);

        User user = userRepository.findByUsername(username.trim()).orElse(null);
        if (user == null || !PasswordHasher.matches(password, user.getSalt(), user.getPasswordHash())) {
            recordFailure(key);
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        if (!user.isEnabled()) {
            throw BusinessException.forbidden("账号已停用，请联系管理员");
        }
        attempts.remove(key);

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        CurrentUser actor = new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole(), user.getDepartment());
        auditService.record(actor, null, AuditAction.LOGIN,
                "登录成功（" + user.getRole().getLabel() + " / " + user.getDepartment() + "）");

        return AuthDtos.LoginResponse.builder()
                .token(tokenService.issue(user))
                .expireAt(tokenService.expireAtText())
                .user(toView(user))
                .build();
    }

    public AuthDtos.UserView me(CurrentUser current) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在"));
        return toView(user);
    }

    public AuthDtos.UserView toView(User user) {
        Map<String, Boolean> prefs = user.getNotificationPrefs() == null
                ? new HashMap<String, Boolean>() : new HashMap<String, Boolean>(user.getNotificationPrefs());
        String avatarUrl = null;
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            avatarUrl = "/api/avatars/" + user.getId() + "?v=" + user.getAvatar();
        }
        return AuthDtos.UserView.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .roleLabel(user.getRole().getLabel())
                .department(user.getDepartment())
                .title(user.getTitle())
                .phone(user.getPhone())
                .email(user.getEmail())
                .bio(user.getBio())
                .avatar(user.getAvatar())
                .avatarUrl(avatarUrl)
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .notificationPrefs(prefs)
                .build();
    }

    private void checkLocked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt != null && attempt.lockedUntil > System.currentTimeMillis()) {
            long minutes = Math.max(1, (attempt.lockedUntil - System.currentTimeMillis()) / 60000 + 1);
            throw BusinessException.forbidden("登录失败次数过多，请 " + minutes + " 分钟后再试");
        }
    }

    private void recordFailure(String key) {
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        attempt.count++;
        if (attempt.count >= MAX_FAILURES) {
            attempt.lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
            attempt.count = 0;
        }
    }

    private static class Attempt {
        private int count;
        private long lockedUntil;
    }
}
