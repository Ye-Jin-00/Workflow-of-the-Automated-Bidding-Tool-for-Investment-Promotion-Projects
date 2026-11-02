package com.sincit.bid.auth;

import com.sincit.bid.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录人快照，由拦截器放入线程上下文，请求结束即清理。
 */
@Data
@AllArgsConstructor
public class CurrentUser {

    private String id;
    private String username;
    private String displayName;
    private UserRole role;
    private String department;

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
