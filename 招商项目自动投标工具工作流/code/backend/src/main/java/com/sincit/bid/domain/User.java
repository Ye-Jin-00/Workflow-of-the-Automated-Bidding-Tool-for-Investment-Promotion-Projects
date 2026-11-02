package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统用户。密码以随机盐 + SHA-256 存储，不保存明文。
 * tokenVersion 用于“修改密码 / 管理员重置密码”后使旧令牌立即失效。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id;
    private String username;
    private String passwordHash;
    private String salt;
    private String displayName;
    private UserRole role;
    private String department;
    private String title;
    private String phone;
    private String email;
    private String bio;
    /** 头像文件名，物理文件位于 data/avatars 下 */
    private String avatar;
    private boolean enabled;
    @Builder.Default
    private int tokenVersion = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    /** 通知偏好：reviewTask / reviewResult / comment / deadline / status */
    @Builder.Default
    private Map<String, Boolean> notificationPrefs = new HashMap<String, Boolean>();

    public boolean isNotifyEnabled(String key) {
        if (notificationPrefs == null || !notificationPrefs.containsKey(key)) {
            return true;
        }
        return Boolean.TRUE.equals(notificationPrefs.get(key));
    }
}
