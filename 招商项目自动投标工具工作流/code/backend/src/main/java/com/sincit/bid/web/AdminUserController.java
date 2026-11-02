package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.service.AdminUserService;
import com.sincit.bid.web.dto.AdminUserDtos;
import com.sincit.bid.web.dto.AuthDtos;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端成员管理。所有接口在 Service 层校验管理员权限。
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AuthDtos.UserView>> list() {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(adminUserService.list(actor));
    }

    @PostMapping
    public ApiResponse<AuthDtos.UserView> create(@RequestBody AdminUserDtos.CreateRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(adminUserService.create(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AuthDtos.UserView> update(@PathVariable String id,
                                                 @RequestBody AdminUserDtos.UpdateRequest request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(adminUserService.update(actor, id, request));
    }

    @PostMapping("/{id}/toggle")
    public ApiResponse<AuthDtos.UserView> toggle(@PathVariable String id) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(adminUserService.toggle(actor, id));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<AdminUserDtos.ResetPasswordResponse> resetPassword(@PathVariable String id) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(adminUserService.resetPassword(actor, id));
    }
}
