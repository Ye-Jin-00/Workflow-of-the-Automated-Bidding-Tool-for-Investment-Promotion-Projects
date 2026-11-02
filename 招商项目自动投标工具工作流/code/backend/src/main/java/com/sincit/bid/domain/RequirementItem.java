package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从招标文件中抽取出的一个要素。source 保留原文片段，便于人工回溯核对。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementItem {

    private String id;
    /** 资格条件 / 技术需求 / 商务条款 / 报价要求 / 时间节点 / 其他 */
    private String category;
    private String title;
    private String content;
    private boolean mandatory;
    private String source;
    /** 规则引擎抽取的置信度 0-1，低于 0.7 的条目建议人工确认 */
    private double confidence;
    private boolean confirmed;
}
