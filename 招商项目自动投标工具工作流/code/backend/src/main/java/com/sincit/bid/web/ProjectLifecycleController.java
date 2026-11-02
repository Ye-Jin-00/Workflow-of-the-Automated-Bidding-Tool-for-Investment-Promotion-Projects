package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.service.ProjectLifecycleService;
import com.sincit.bid.web.dto.BatchDtos;
import org.springframework.web.bind.annotation.*;

/**
 * 项目删除入口。删除为软删除，进入回收站保留 30 天。
 */
@RestController
public class ProjectLifecycleController {

    private final ProjectLifecycleService lifecycleService;

    public ProjectLifecycleController(ProjectLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @RequestParam(required = false) String reason) {
        CurrentUser actor = UserContext.require();
        lifecycleService.delete(id, reason, actor);
        return ApiResponse.ok();
    }

    @PostMapping("/projects/batch/delete")
    public ApiResponse<BatchDtos.BatchResult> batchDelete(@RequestBody BatchDtos.DeleteRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(lifecycleService.batchDelete(request.getIds(), request.getReason(), actor));
    }
}
