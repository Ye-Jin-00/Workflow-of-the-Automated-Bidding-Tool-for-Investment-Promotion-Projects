package com.sincit.bid.web.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理端成员管理 DTO。
 */
public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    @Data
    public static class CreateRequest {
        private String username;
        private String password;
        private String displayName;
        private String role;
        private String department;
        private String title;
        private String phone;
        private String email;
    }

    @Data
    public static class UpdateRequest {
        private String displayName;
        private String role;
        private String department;
        private String title;
        private String phone;
        private String email;
        private String bio;
    }

    @Data
    public static class ResetPasswordResponse {
        private String password;
        private AuthDtos.UserView user;
    }

    @Data
    public static class UserList {
        private List<AuthDtos.UserView> items;
        private long total;
    }
}
