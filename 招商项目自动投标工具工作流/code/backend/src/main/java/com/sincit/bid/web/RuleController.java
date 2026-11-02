package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.service.RuleService;
import com.sincit.bid.web.dto.RuleDtos;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public ApiResponse<List<RuleDtos.RuleView>> list() {
        return ApiResponse.ok(ruleService.list());
    }

    @PutMapping("/{code}")
    public ApiResponse<RuleDtos.RuleView> update(@PathVariable String code,
                                                 @RequestBody RuleDtos.UpdateRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(ruleService.update(code, request, actor));
    }

    @PostMapping("/reset")
    public ApiResponse<List<RuleDtos.RuleView>> reset() {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(ruleService.reset(actor));
    }
}
