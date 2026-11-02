package com.sincit.bid.domain;

/**
 * 系统角色。数据权限在 PermissionService 中集中实现，避免散落到各接口。
 */
public enum UserRole {

    ADMIN("系统管理员"),
    BID_MANAGER("投标经理"),
    REVIEWER("复核/风控"),
    VIEWER("只读观察员");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
