package com.sincit.bid.auth;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.config.AppProperties;
import com.sincit.bid.domain.User;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 轻量 HMAC-SHA256 令牌。payload = userId.expireAt.tokenVersion。
 * tokenVersion 与用户记录比对：修改密码或管理员重置密码后，旧令牌立即失效。
 */
@Component
public class TokenService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppProperties properties;

    public TokenService(AppProperties properties) {
        this.properties = properties;
    }

    public String issue(User user) {
        long expireAt = System.currentTimeMillis() + properties.getTokenExpireHours() * 3600_000L;
        String payload = user.getId() + "." + expireAt + "." + user.getTokenVersion();
        return payload + "." + sign(payload);
    }

    public TokenPayload parse(String token) {
        if (token == null || token.isEmpty()) {
            throw BusinessException.unauthorized("缺少登录凭证");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 4) {
            throw BusinessException.unauthorized("登录凭证格式不正确，请重新登录");
        }
        String payload = parts[0] + "." + parts[1] + "." + parts[2];
        if (!constantEquals(sign(payload), parts[3])) {
            throw BusinessException.unauthorized("登录凭证签名无效");
        }
        long expireAt;
        int tokenVersion;
        try {
            expireAt = Long.parseLong(parts[1]);
            tokenVersion = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw BusinessException.unauthorized("登录凭证格式不正确");
        }
        if (System.currentTimeMillis() > expireAt) {
            throw BusinessException.unauthorized("登录已过期，请重新登录");
        }
        return new TokenPayload(parts[0], tokenVersion);
    }

    public String expireAtText() {
        long expireAt = System.currentTimeMillis() + properties.getTokenExpireHours() * 3600_000L;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(expireAt), ZoneId.systemDefault()).format(FORMATTER);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("令牌签名失败", e);
        }
    }

    private boolean constantEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    public static class TokenPayload {
        private final String userId;
        private final int tokenVersion;

        public TokenPayload(String userId, int tokenVersion) {
            this.userId = userId;
            this.tokenVersion = tokenVersion;
        }

        public String getUserId() {
            return userId;
        }

        public int getTokenVersion() {
            return tokenVersion;
        }
    }
}
