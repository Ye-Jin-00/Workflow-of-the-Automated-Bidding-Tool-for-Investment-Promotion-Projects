package com.sincit.bid.web;

import com.sincit.bid.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("status", "UP");
        data.put("service", "bid-workflow");
        return ApiResponse.ok(data);
    }
}
