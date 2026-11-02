package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资质匹配报告：把企业能力与招标要求逐项比对，并给出投标建议。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchReport {

    private int score;
    /** 强烈建议 / 建议投标 / 谨慎评估 / 不建议 */
    private String recommendation;
    @Builder.Default
    private List<MatchDimension> dimensions = new ArrayList<MatchDimension>();
    @Builder.Default
    private List<String> matched = new ArrayList<String>();
    @Builder.Default
    private List<String> gaps = new ArrayList<String>();
    private String summary;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchDimension {
        private String name;
        private int score;
        private int fullScore;
        private String comment;
    }
}
