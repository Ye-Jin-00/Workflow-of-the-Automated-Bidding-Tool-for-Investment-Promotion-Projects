package com.sincit.bid.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.common.ErrorCode;
import com.sincit.bid.domain.User;
import com.sincit.bid.persistence.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * Bearer Token 认证拦截器。认证失败也返回统一 JSON 结构，前端无需特殊分支。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(TokenService tokenService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "请先登录");
            return false;
        }
        try {
            TokenService.TokenPayload payload = tokenService.parse(header.substring(7).trim());
            User user = userRepository.findById(payload.getUserId())
                    .orElseThrow(() -> BusinessException.unauthorized("用户不存在或已删除"));
            if (!user.isEnabled()) {
                throw BusinessException.unauthorized("账号已停用");
            }
            if (user.getTokenVersion() != payload.getTokenVersion()) {
                throw BusinessException.unauthorized("登录已失效，请重新登录");
            }
            UserContext.set(new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(),
                    user.getRole(), user.getDepartment()));
            request.setAttribute("clientIp", clientIp(request));
            return true;
        } catch (BusinessException e) {
            writeUnauthorized(response, e.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.UNAUTHORIZED.getCode(), message, null);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(body));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
