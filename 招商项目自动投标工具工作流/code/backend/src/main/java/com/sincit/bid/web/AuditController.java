package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.domain.AuditLog;
import com.sincit.bid.service.AuditQueryService;
import com.sincit.bid.web.dto.PageResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/audits")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    public ApiResponse<PageResult<AuditLog>> list(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(auditQueryService.query(projectId, action, keyword, page, size, actor));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword) {
        CurrentUser actor = UserContext.require();
        List<AuditLog> logs = auditQueryService.export(projectId, action, keyword, actor);
        StringBuilder csv = new StringBuilder();
        csv.append("时间,项目编号,操作人,角色,动作,详情,IP,追踪号\n");
        for (AuditLog log : logs) {
            csv.append(cell(log.getCreatedAt() == null ? "" : log.getCreatedAt().toString())).append(',')
                    .append(cell(log.getProjectCode())).append(',')
                    .append(cell(log.getActorName())).append(',')
                    .append(cell(log.getActorRole())).append(',')
                    .append(cell(log.getActionLabel())).append(',')
                    .append(cell(log.getDetail())).append(',')
                    .append(cell(log.getIp())).append(',')
                    .append(cell(log.getTraceId())).append('\n');
        }
        byte[] body = ("\ufeff" + csv).getBytes(StandardCharsets.UTF_8);
        String filename = "audit-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }

    private String cell(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"").replace("\n", " ") + "\"";
    }
}
