package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.service.ProjectLifecycleService;
import com.sincit.bid.web.dto.BatchDtos;
import com.sincit.bid.web.dto.RecycleBinDtos;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 回收站：仅管理员可见，内容最多保留 30 天。
 */
@RestController
@RequestMapping("/admin/recycle-bin")
public class RecycleBinController {

    private final ProjectLifecycleService lifecycleService;

    public RecycleBinController(ProjectLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @GetMapping
    public ApiResponse<List<RecycleBinDtos.RecycleItem>> list() {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(lifecycleService.list(actor));
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<RecycleBinDtos.RecycleItem> restore(@PathVariable String id) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(lifecycleService.restore(id, actor));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> purge(@PathVariable String id) {
        CurrentUser actor = UserContext.require();
        lifecycleService.purge(id, actor);
        return ApiResponse.ok();
    }

    @PostMapping("/batch/restore")
    public ApiResponse<BatchDtos.BatchResult> batchRestore(@RequestBody BatchDtos.IdsRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(lifecycleService.batchRestore(request.getIds(), actor));
    }

    @PostMapping("/batch/purge")
    public ApiResponse<BatchDtos.BatchResult> batchPurge(@RequestBody BatchDtos.IdsRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(lifecycleService.batchPurge(request.getIds(), actor));
    }
}
