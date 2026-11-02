package com.sincit.bid.domain;

/**
 * 审计动作字典。新增动作时同步维护中文标签，保证日志可读。
 */
public enum AuditAction {

    LOGIN("登录系统"),
    LOGOUT("退出登录"),
    PROFILE_UPDATE("修改个人资料"),
    PASSWORD_CHANGE("修改密码"),
    AVATAR_UPDATE("更新头像"),
    PREFERENCES_UPDATE("修改通知偏好"),
    USER_CREATE("新增成员"),
    USER_UPDATE("编辑成员"),
    USER_DISABLE("停用成员"),
    USER_ENABLE("启用成员"),
    USER_RESET_PASSWORD("重置成员密码"),
    PROJECT_CREATE("创建项目"),
    PROJECT_DUPLICATE("复制项目"),
    PROJECT_UPDATE("修改项目"),
    PROJECT_DELETE("删除项目"),
    PROJECT_RESTORE("恢复项目"),
    PROJECT_PURGE("彻底删除项目"),
    QUALIFICATION_CHECK("资格预审"),
    AUTO_REJECT("资格不满足自动驳回"),
    EXTRACT("招标要素抽取"),
    MATCH_PRICE("资质匹配与报价测算"),
    AI_DRAFT("AI生成标书初稿"),
    DRAFT_UPDATE("人工编辑标书初稿"),
    SUBMIT_REVIEW("提交人工复核"),
    REVIEW_APPROVE("人工复核通过"),
    REVIEW_RETURN("人工复核退回"),
    SUBMIT("提交投标"),
    ARCHIVE("项目归档"),
    ATTACHMENT_UPLOAD("上传项目文件"),
    ATTACHMENT_DELETE("删除项目文件"),
    COMMENT_ADD("添加项目评论"),
    COMMENT_DELETE("删除项目评论"),
    FOLLOW("关注项目"),
    UNFOLLOW("取消关注"),
    RULE_UPDATE("更新资格规则"),
    PROJECT_CANCEL("取消项目");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
