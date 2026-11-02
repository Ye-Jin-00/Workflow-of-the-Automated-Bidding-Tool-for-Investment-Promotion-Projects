package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.domain.AuditLog;
import com.sincit.bid.service.ProfileService;
import com.sincit.bid.web.dto.AuthDtos;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 个人中心接口。
 */
@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<AuthDtos.UserView> view() {
        return ApiResponse.ok(profileService.view(UserContext.require()));
    }

    @PutMapping
    public ApiResponse<AuthDtos.UserView> update(@RequestBody AuthDtos.ProfileUpdateRequest request) {
        return ApiResponse.ok(profileService.updateProfile(UserContext.require(), request));
    }

    @PutMapping("/password")
    public ApiResponse<AuthDtos.PasswordChangeResponse> changePassword(
            @RequestBody AuthDtos.PasswordChangeRequest request) {
        return ApiResponse.ok(profileService.changePassword(UserContext.require(), request));
    }

    @PostMapping("/avatar")
    public ApiResponse<AuthDtos.UserView> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(profileService.updateAvatar(UserContext.require(), file));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<AuthDtos.UserView> removeAvatar() {
        return ApiResponse.ok(profileService.removeAvatar(UserContext.require()));
    }

    @PutMapping("/preferences")
    public ApiResponse<Map<String, Boolean>> preferences(@RequestBody AuthDtos.PreferencesRequest request) {
        return ApiResponse.ok(profileService.updatePreferences(UserContext.require(), request.getPreferences()));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(profileService.summary(UserContext.require()));
    }

    @GetMapping("/activity")
    public ApiResponse<List<AuditLog>> activity() {
        return ApiResponse.ok(profileService.activity(UserContext.require()));
    }
}
