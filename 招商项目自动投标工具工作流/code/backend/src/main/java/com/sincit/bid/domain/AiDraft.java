package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 生成的标书初稿。degraded=true 表示大模型不可用，已使用本地模板降级生成。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDraft {

    @Builder.Default
    private List<Section> outline = new ArrayList<Section>();
    private String technicalProposal;
    private String commercialResponse;
    private String qualificationResponse;
    @Builder.Default
    private List<String> riskNotes = new ArrayList<String>();
    private String provider;
    private String model;
    private String promptVersion;
    private LocalDateTime generatedAt;
    private boolean degraded;
    private long elapsedMs;
    private String warning;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        private String title;
        @Builder.Default
        private List<String> points = new ArrayList<String>();
    }
}
