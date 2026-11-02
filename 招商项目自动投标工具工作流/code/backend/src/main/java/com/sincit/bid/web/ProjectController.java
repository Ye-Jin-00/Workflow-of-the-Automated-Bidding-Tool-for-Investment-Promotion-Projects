package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.domain.AuditLog;
import com.sincit.bid.service.AuditQueryService;
import com.sincit.bid.service.ProjectService;
import com.sincit.bid.web.dto.PageResult;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目查询、创建与编辑接口。
 */
@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final AuditQueryService auditQueryService;

    public ProjectController(ProjectService projectService, AuditQueryService auditQueryService) {
        this.projectService = projectService;
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    public ApiResponse<PageResult<ProjectDtos.Summary>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean following,
            @RequestParam(required = false) Boolean includeDraft,
            @RequestParam(required = false) String view,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(projectService.list(keyword, status, department, following, includeDraft,
                view, page, size, actor));
    }

    @PostMapping
    public ApiResponse<ProjectDtos.Detail> create(@RequestBody ProjectDtos.SaveRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(projectService.create(request, actor));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDtos.Detail> detail(@PathVariable String id) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(projectService.detail(id, actor));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectDtos.Detail> update(@PathVariable String id,
                                                  @RequestBody ProjectDtos.SaveRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(projectService.update(id, request, actor));
    }

    @GetMapping("/{id}/timeline")
    public ApiResponse<List<AuditLog>> timeline(@PathVariable String id) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(auditQueryService.timeline(id, actor));
    }
}
