package com.sincit.bid.service;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.common.TraceIdFilter;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.AuditLog;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.persistence.AuditRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计服务：所有关键动作统一从这里落库，禁止业务代码自行拼装日志。
 */
@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public AuditLog record(CurrentUser actor, BidProject project, AuditAction action, String detail) {
        AuditLog log = AuditLog.builder()
                .id("AL" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .projectId(project == null ? null : project.getId())
                .projectCode(project == null ? null : project.getCode())
                .actorId(actor == null ? "system" : actor.getId())
                .actorName(actor == null ? "系统" : actor.getDisplayName())
                .actorRole(actor == null ? "SYSTEM" : actor.getRole().name())
                .action(action.name())
                .actionLabel(action.getLabel())
                .detail(detail)
                .ip(currentIp())
                .traceId(MDC.get(TraceIdFilter.TRACE_ID))
                .createdAt(LocalDateTime.now())
                .build();
        auditRepository.append(log);
        return log;
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            Object clientIp = request.getAttribute("clientIp");
            return clientIp == null ? request.getRemoteAddr() : String.valueOf(clientIp);
        } catch (Exception e) {
            return null;
        }
    }
}
