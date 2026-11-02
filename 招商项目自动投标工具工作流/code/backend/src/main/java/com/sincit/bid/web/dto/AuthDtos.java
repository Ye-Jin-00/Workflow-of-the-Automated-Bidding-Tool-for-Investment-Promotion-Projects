package com.sincit.bid.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证与个人中心 DTO。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private Boolean rememberMe;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String expireAt;
        private UserView user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserView {
        private String id;
        private String username;
        private String displayName;
        private String role;
        private String roleLabel;
        private String department;
        private String title;
        private String phone;
        private String email;
        private String bio;
        private String avatarUrl;
        private String avatar;
        private boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private Map<String, Boolean> notificationPrefs;
    }

    @Data
    public static class ProfileUpdateRequest {
        private String displayName;
        private String title;
        private String phone;
        private String email;
        private String bio;
    }

    @Data
    public static class PasswordChangeRequest {
        private String oldPassword;
        private String newPassword;
        private String confirmPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordChangeResponse {
        private String token;
        private String expireAt;
    }

    @Data
    public static class PreferencesRequest {
        private Map<String, Boolean> preferences;
    }
}
