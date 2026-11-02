package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.service.WorkflowService;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流动作接口。所有动作都需要版本号，避免并发“双写”造成状态覆盖。
 */
@RestController
@RequestMapping("/projects/{id}")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/qualification-check")
    public ApiResponse<ProjectDtos.Detail> qualificationCheck(@PathVariable String id,
                                                              @RequestBody ProjectDtos.QualificationRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.qualificationCheck(id, request, actor));
    }

    @PostMapping("/extract")
    public ApiResponse<ProjectDtos.Detail> extract(@PathVariable String id,
                                                   @RequestBody ProjectDtos.ExtractRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.extract(id, request, actor));
    }

    @PostMapping("/match-price")
    public ApiResponse<ProjectDtos.Detail> matchPrice(@PathVariable String id,
                                                      @RequestBody ProjectDtos.MatchPriceRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.matchAndPrice(id, request, actor));
    }

    @PostMapping("/ai-draft")
    public ApiResponse<ProjectDtos.Detail> aiDraft(@PathVariable String id,
                                                   @RequestBody ProjectDtos.AiDraftRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.generateDraft(id, request, actor));
    }

    @PutMapping("/draft")
    public ApiResponse<ProjectDtos.Detail> updateDraft(@PathVariable String id,
                                                       @RequestBody ProjectDtos.DraftUpdateRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.updateDraft(id, request, actor));
    }

    @PostMapping("/submit-review")
    public ApiResponse<ProjectDtos.Detail> submitReview(@PathVariable String id,
                                                        @RequestParam(required = false) Integer version) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.submitReview(id, version, actor));
    }

    @PostMapping("/review")
    public ApiResponse<ProjectDtos.Detail> review(@PathVariable String id,
                                                  @RequestBody ProjectDtos.ReviewRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.review(id, request, actor));
    }

    @PostMapping("/submit")
    public ApiResponse<ProjectDtos.Detail> submit(@PathVariable String id,
                                                  @RequestBody ProjectDtos.SubmitRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.submit(id, request, actor));
    }

    @PostMapping("/archive")
    public ApiResponse<ProjectDtos.Detail> archive(@PathVariable String id,
                                                   @RequestBody ProjectDtos.ArchiveRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(workflowService.archive(id, request, actor));
    }
}
