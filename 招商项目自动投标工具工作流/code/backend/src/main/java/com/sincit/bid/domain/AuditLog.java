package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志。工作流每一次状态变化、AI 生成、人工复核与数据修改都留痕，满足事后追溯要求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    private String id;
    private String projectId;
    private String projectCode;
    private String actorId;
    private String actorName;
    private String actorRole;
    private String action;
    private String actionLabel;
    private String detail;
    private String ip;
    private String traceId;
    private LocalDateTime createdAt;
}
