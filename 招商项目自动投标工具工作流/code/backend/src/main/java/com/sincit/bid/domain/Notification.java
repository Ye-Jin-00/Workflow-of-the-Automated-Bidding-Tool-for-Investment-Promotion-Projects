package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内消息。用于复核任务、复核结果、截止提醒、评论与状态变更通知。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private String id;
    private String userId;
    private NotificationType type;
    private String title;
    private String content;
    private String projectId;
    private String projectCode;
    /** 前端点击后跳转路径，如 /projects/P123 */
    private String link;
    private boolean read;
    private String dedupeKey;
    private LocalDateTime createdAt;

    public enum NotificationType {
        REVIEW_TASK("待复核任务"),
        REVIEW_RESULT("复核结果"),
        DEADLINE("截止提醒"),
        COMMENT("项目评论"),
        ATTACHMENT("项目文件"),
        STATUS("状态变更"),
        SYSTEM("系统通知");

        private final String label;

        NotificationType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
