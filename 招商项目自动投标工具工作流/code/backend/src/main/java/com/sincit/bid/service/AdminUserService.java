package com.sincit.bid.service;

import com.sincit.bid.auth.AuthService;
import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PasswordHasher;
import com.sincit.bid.auth.PasswordPolicy;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.User;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.web.dto.AdminUserDtos;
import com.sincit.bid.web.dto.AuthDtos;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * 管理员成员管理：新增、编辑、启停、重置密码。
 * 关键安全约束：不能停用自己、不能移除最后一个管理员。
 */
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final PermissionService permissionService;

    public AdminUserService(UserRepository userRepository, AuthService authService,
                            AuditService auditService, PermissionService permissionService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.permissionService = permissionService;
    }

    public List<AuthDtos.UserView> list(CurrentUser actor) {
        permissionService.assertAdmin(actor);
        List<User> users = new ArrayList<User>(userRepository.findAll());
        users.sort(Comparator.comparing((User u) -> u.getRole().ordinal()).thenComparing(User::getId));
        List<AuthDtos.UserView> views = new ArrayList<AuthDtos.UserView>();
        for (User user : users) {
            views.add(authService.toView(user));
        }
        return views;
    }

    public AuthDtos.UserView create(CurrentUser actor, AdminUserDtos.CreateRequest request) {
        permissionService.assertAdmin(actor);
        String username = trim(request.getUsername()).toLowerCase(Locale.ROOT);
        if (!username.matches("^[a-z][a-z0-9_]{2,19}$")) {
            throw BusinessException.invalid("用户名需为 3-20 位小写字母、数字或下划线，并以字母开头");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw BusinessException.invalid("用户名已存在");
        }
        PasswordPolicy.validate(request.getPassword());
        String displayName = trim(request.getDisplayName());
        if (displayName.length() < 2 || displayName.length() > 20) {
            throw BusinessException.invalid("姓名长度需为 2-20 个字符");
        }
        UserRole role = parseRole(request.getRole());
        String department = trim(request.getDepartment());
        if (department.isEmpty()) {
            throw BusinessException.invalid("请选择部门");
        }
        validateContact(request.getPhone(), request.getEmail());

        String salt = PasswordHasher.newSalt();
        User user = User.builder()
                .id("U" + System.currentTimeMillis())
                .username(username)
                .salt(salt)
                .passwordHash(PasswordHasher.hash(request.getPassword(), salt))
                .displayName(displayName)
                .role(role)
                .department(department)
                .title(trimNullable(request.getTitle()))
                .phone(trimNullable(request.getPhone()))
                .email(trimNullable(request.getEmail()))
                .enabled(true)
                .tokenVersion(0)
                .notificationPrefs(new HashMap<String, Boolean>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        auditService.record(actor, null, AuditAction.USER_CREATE,
                "新增成员 " + username + "（" + role.getLabel() + " / " + department + "）");
        return authService.toView(user);
    }

    public AuthDtos.UserView update(CurrentUser actor, String id, AdminUserDtos.UpdateRequest request) {
        permissionService.assertAdmin(actor);
        User user = get(id);
        UserRole newRole = request.getRole() == null || request.getRole().trim().isEmpty()
                ? user.getRole() : parseRole(request.getRole());
        if (user.getId().equals(actor.getId()) && newRole != user.getRole()) {
            throw BusinessException.invalid("不能修改自己的角色");
        }
        if (user.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN && countEnabledAdmins(user.getId()) == 0) {
            throw BusinessException.invalid("至少需要保留一名启用状态的管理员");
        }
        if (request.getDisplayName() != null) {
            String name = request.getDisplayName().trim();
            if (name.length() < 2 || name.length() > 20) {
                throw BusinessException.invalid("姓名长度需为 2-20 个字符");
            }
            user.setDisplayName(name);
        }
        if (request.getDepartment() != null && !request.getDepartment().trim().isEmpty()) {
            user.setDepartment(request.getDepartment().trim());
        }
        validateContact(request.getPhone(), request.getEmail());
        user.setRole(newRole);
        if (request.getTitle() != null) {
            user.setTitle(trimNullable(request.getTitle()));
        }
        if (request.getPhone() != null) {
            user.setPhone(trimNullable(request.getPhone()));
        }
        if (request.getEmail() != null) {
            user.setEmail(trimNullable(request.getEmail()));
        }
        if (request.getBio() != null) {
            user.setBio(trimNullable(request.getBio()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.record(actor, null, AuditAction.USER_UPDATE,
                "编辑成员 " + user.getUsername() + "（" + user.getRole().getLabel() + " / " + user.getDepartment() + "）");
        return authService.toView(user);
    }

    public AuthDtos.UserView toggle(CurrentUser actor, String id) {
        permissionService.assertAdmin(actor);
        if (actor.getId().equals(id)) {
            throw BusinessException.invalid("不能停用当前登录账号");
        }
        User user = get(id);
        if (user.isEnabled() && user.getRole() == UserRole.ADMIN && countEnabledAdmins(user.getId()) == 0) {
            throw BusinessException.invalid("至少需要保留一名启用状态的管理员");
        }
        user.setEnabled(!user.isEnabled());
        if (!user.isEnabled()) {
            // 停用时递增版本，避免账号重新启用后旧令牌继续可用
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.record(actor, null,
                user.isEnabled() ? AuditAction.USER_ENABLE : AuditAction.USER_DISABLE,
                (user.isEnabled() ? "启用" : "停用") + "成员 " + user.getUsername());
        return authService.toView(user);
    }

    public AdminUserDtos.ResetPasswordResponse resetPassword(CurrentUser actor, String id) {
        permissionService.assertAdmin(actor);
        User user = get(id);
        String password = PasswordPolicy.generate();
        String salt = PasswordHasher.newSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordHasher.hash(password, salt));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.record(actor, null, AuditAction.USER_RESET_PASSWORD,
                "重置成员 " + user.getUsername() + " 的密码，旧令牌已失效");
        AdminUserDtos.ResetPasswordResponse response = new AdminUserDtos.ResetPasswordResponse();
        response.setPassword(password);
        response.setUser(authService.toView(user));
        return response;
    }

    private User get(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("成员不存在"));
    }

    private UserRole parseRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw BusinessException.invalid("请选择角色");
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid("角色不合法");
        }
    }

    private long countEnabledAdmins(String excludeId) {
        return userRepository.findAll().stream()
                .filter(u -> u.isEnabled() && u.getRole() == UserRole.ADMIN && !u.getId().equals(excludeId))
                .count();
    }

    private void validateContact(String phone, String email) {
        if (phone != null && !phone.trim().isEmpty() && !phone.trim().matches("^1[3-9]\\d{9}$")) {
            throw BusinessException.invalid("手机号格式不正确");
        }
        if (email != null && !email.trim().isEmpty()
                && !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw BusinessException.invalid("邮箱格式不正确");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
