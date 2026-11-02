package com.sincit.bid.common;

/**
 * 业务错误码。0 表示成功，其余按领域分段。
 */
public enum ErrorCode {

    OK(0, "success"),
    INVALID_PARAM(1001, "请求参数不合法"),
    UNAUTHORIZED(1002, "登录已失效，请重新登录"),
    FORBIDDEN(1003, "无权执行该操作"),
    NOT_FOUND(1004, "数据不存在"),
    STATE_CONFLICT(1005, "当前状态不允许该操作"),
    DUPLICATE(1006, "数据重复"),
    AI_DEGRADED(2001, "大模型不可用，已降级为本地生成"),
    INTERNAL_ERROR(5000, "系统内部错误");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
