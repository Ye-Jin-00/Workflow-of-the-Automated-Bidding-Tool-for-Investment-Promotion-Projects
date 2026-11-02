package com.sincit.bid.ai;

import com.sincit.bid.config.AppProperties;
import com.sincit.bid.domain.AiDraft;
import com.sincit.bid.domain.BidProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI 生成编排：选择供应商、计时、处理降级。
 * 设计原则：AI 是增强项而非单点依赖，任何失败都必须产出可用初稿并明确标注。
 */
@Service
public class AiDraftService {

    private static final Logger log = LoggerFactory.getLogger(AiDraftService.class);

    private final AppProperties properties;
    private final LocalTemplateAiProvider localProvider;
    private final OpenAiCompatibleAiProvider remoteProvider;

    public AiDraftService(AppProperties properties, LocalTemplateAiProvider localProvider,
                          OpenAiCompatibleAiProvider remoteProvider) {
        this.properties = properties;
        this.localProvider = localProvider;
        this.remoteProvider = remoteProvider;
    }

    public AiDraft generate(BidProject project, String emphasis, String tone) {
        long start = System.currentTimeMillis();
        boolean remoteConfigured = "openai-compatible".equalsIgnoreCase(properties.getAi().getProvider())
                && properties.getAi().getApiKey() != null
                && !properties.getAi().getApiKey().trim().isEmpty();

        AiDraft draft;
        if (!remoteConfigured) {
            draft = localProvider.generate(project, emphasis, tone);
            if ("openai-compatible".equalsIgnoreCase(properties.getAi().getProvider())) {
                draft.setDegraded(true);
                draft.setWarning("未配置大模型 API Key，已自动使用本地模板生成初稿；配置后可在“重新生成”中切换。");
            } else {
                draft.setWarning("当前使用本地模板引擎生成，内容可人工编辑；如需大模型润色，请配置 app.ai.provider。");
            }
        } else {
            try {
                draft = remoteProvider.generate(project, emphasis, tone);
            } catch (Exception e) {
                log.warn("AI provider failed, fallback to local template: {}", e.getMessage());
                draft = localProvider.generate(project, emphasis, tone);
                draft.setDegraded(true);
                draft.setWarning("大模型调用失败，已降级为本地模板生成：" + safeMessage(e));
            }
        }
        if (draft.getGeneratedAt() == null) {
            draft.setGeneratedAt(LocalDateTime.now());
        }
        draft.setElapsedMs(System.currentTimeMillis() - start);
        return draft;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.length() > 120 ? message.substring(0, 120) : message;
    }
}
