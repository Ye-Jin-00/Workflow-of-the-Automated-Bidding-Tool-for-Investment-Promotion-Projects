package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.domain.Notification;
import com.sincit.bid.service.NotificationService;
import com.sincit.bid.web.dto.PageResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResult<Notification>> list(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(notificationService.list(actor, unreadOnly, type, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount() {
        CurrentUser actor = UserContext.require();
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("count", notificationService.unreadCount(actor));
        return ApiResponse.ok(data);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable String id) {
        notificationService.markRead(id, UserContext.require());
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Object>> readAll() {
        int count = notificationService.markAllRead(UserContext.require());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("count", count);
        return ApiResponse.ok(data);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        notificationService.delete(id, UserContext.require());
        return ApiResponse.ok();
    }
}
