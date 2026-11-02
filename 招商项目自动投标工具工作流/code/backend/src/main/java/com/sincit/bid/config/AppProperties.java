package com.sincit.bid.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置。密钥与 AI Key 支持通过环境变量覆盖，避免写入代码仓库。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String dataDir = "./data";
    private String tokenSecret = "change-me-in-production";
    private int tokenExpireHours = 12;
    private Ai ai = new Ai();

    @Data
    public static class Ai {
        /** local / openai-compatible */
        private String provider = "local";
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";
        private String apiKey = "";
        private int connectTimeoutSeconds = 8;
        private int readTimeoutSeconds = 45;
    }
}
