package com.sincit.bid.web;

import com.sincit.bid.auth.AuthService;
import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.service.AuditService;
import com.sincit.bid.web.dto.AuthDtos;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口。除登录外均需 Bearer Token。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResponse> login(@RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserView> me() {
        CurrentUser current = UserContext.require();
        return ApiResponse.ok(authService.me(current));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        CurrentUser actor = UserContext.get();
        if (actor != null) {
            auditService.record(actor, null, AuditAction.LOGOUT, "退出登录");
        }
        return ApiResponse.ok();
    }
}
