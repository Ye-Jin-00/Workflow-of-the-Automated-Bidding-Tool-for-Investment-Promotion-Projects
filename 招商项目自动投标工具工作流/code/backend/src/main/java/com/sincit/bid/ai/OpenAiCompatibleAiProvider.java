package com.sincit.bid.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sincit.bid.config.AppProperties;
import com.sincit.bid.domain.AiDraft;
import com.sincit.bid.domain.BidProject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议的大模型实现（DeepSeek、通义、Kimi 等均可）。
 * 仅当配置了 API Key 时启用；任何网络/解析异常由上层捕获并降级到本地模板。
 */
@Component
public class OpenAiCompatibleAiProvider implements AiProvider {

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleAiProvider(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getAi().getConnectTimeoutSeconds() * 1000);
        factory.setReadTimeout(properties.getAi().getReadTimeoutSeconds() * 1000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String name() {
        return "openai-compatible";
    }

    @Override
    public AiDraft generate(BidProject project, String emphasis, String tone) {
        String apiKey = properties.getAi().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("未配置 AI API Key");
        }
        String endpoint = endpoint(properties.getAi().getBaseUrl());

        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        messages.add(message("system", PromptBuilder.systemPrompt()));
        messages.add(message("user", PromptBuilder.userPrompt(project, emphasis, tone)));

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", properties.getAi().getModel());
        body.put("messages", messages);
        body.put("temperature", 0.4);
        body.put("stream", false);
        body.put("max_tokens", 4000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<Map<String, Object>>(body, headers),
                String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("大模型返回异常状态: " + response.getStatusCodeValue());
        }
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalStateException("大模型返回内容为空");
            }
            return parse(content);
        } catch (Exception e) {
            throw new IllegalStateException("大模型响应解析失败: " + truncate(e.getMessage(), 160), e);
        }
    }

    private AiDraft parse(String rawContent) throws Exception {
        String content = rawContent.trim();
        if (content.startsWith("```")) {
            int firstLineEnd = content.indexOf('\n');
            content = firstLineEnd > 0 ? content.substring(firstLineEnd + 1) : content;
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(content.trim());
        } catch (Exception e) {
            // 模型未按 JSON 输出时，保留正文，按标题生成大纲
            return AiDraft.builder()
                    .outline(outlineFromText(content))
                    .technicalProposal(content)
                    .commercialResponse("详见技术标商务章节。")
                    .qualificationResponse("详见资格文件。")
                    .riskNotes(new ArrayList<String>())
                    .provider(name())
                    .model(properties.getAi().getModel())
                    .promptVersion(PromptBuilder.PROMPT_VERSION)
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
        List<AiDraft.Section> outline = new ArrayList<AiDraft.Section>();
        JsonNode outlineNode = node.path("outline");
        if (outlineNode.isArray()) {
            for (JsonNode sectionNode : outlineNode) {
                List<String> points = new ArrayList<String>();
                for (JsonNode point : sectionNode.path("points")) {
                    points.add(point.asText());
                }
                outline.add(AiDraft.Section.builder()
                        .title(sectionNode.path("title").asText("章节"))
                        .points(points)
                        .build());
            }
        }
        List<String> risks = new ArrayList<String>();
        for (JsonNode risk : node.path("riskNotes")) {
            risks.add(risk.asText());
        }
        String technical = node.path("technicalProposal").asText();
        if (technical.trim().isEmpty()) {
            throw new IllegalStateException("大模型未返回技术标正文");
        }
        return AiDraft.builder()
                .outline(outline.isEmpty() ? outlineFromText(technical) : outline)
                .technicalProposal(technical)
                .commercialResponse(node.path("commercialResponse").asText("详见商务响应表。"))
                .qualificationResponse(node.path("qualificationResponse").asText("详见资格文件。"))
                .riskNotes(risks)
                .provider(name())
                .model(properties.getAi().getModel())
                .promptVersion(PromptBuilder.PROMPT_VERSION)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private List<AiDraft.Section> outlineFromText(String text) {
        List<AiDraft.Section> outline = new ArrayList<AiDraft.Section>();
        for (String line : text.split("\\n")) {
            String value = line.trim();
            if (value.matches("^[一二三四五六七八九十]+、.{2,30}$")) {
                outline.add(AiDraft.Section.builder()
                        .title(value)
                        .points(new ArrayList<String>())
                        .build());
            }
        }
        return outline;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<String, String>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String endpoint(String baseUrl) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1")) {
            return base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
