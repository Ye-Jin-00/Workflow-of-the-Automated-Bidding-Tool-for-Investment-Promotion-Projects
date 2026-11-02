package com.sincit.bid.domain;

/**
 * 投标项目状态机。stage 对应工作流七个环节，前端用于步骤条展示。
 */
public enum BidStatus {

    DRAFT("草稿", 1, "info"),
    QUALIFICATION_REJECTED("资格预审驳回", 2, "danger"),
    QUALIFICATION_PASSED("资格预审通过", 2, "success"),
    REQUIREMENTS_EXTRACTED("要素已抽取", 3, "success"),
    MATCHED("匹配测算完成", 4, "success"),
    AI_DRAFTED("初稿已生成", 5, "success"),
    REVIEW_PENDING("待人工复核", 6, "warning"),
    REVIEW_REJECTED("复核退回", 6, "danger"),
    APPROVED("复核通过", 6, "success"),
    SUBMITTED("已提交", 7, "success"),
    ARCHIVED("已归档", 7, "info");

    private final String label;
    private final int stage;
    private final String uiType;

    BidStatus(String label, int stage, String uiType) {
        this.label = label;
        this.stage = stage;
        this.uiType = uiType;
    }

    public String getLabel() {
        return label;
    }

    public int getStage() {
        return stage;
    }

    public String getUiType() {
        return uiType;
    }

    public boolean isRejected() {
        return this == QUALIFICATION_REJECTED || this == REVIEW_REJECTED;
    }

    /**
     * 仅草稿与资格驳回阶段允许修改项目基础信息，进入后续阶段后信息冻结，保证留痕可信。
     */
    public boolean isBaseEditable() {
        return this == DRAFT || this == QUALIFICATION_REJECTED;
    }

    public boolean isFinished() {
        return this == SUBMITTED || this == ARCHIVED;
    }
}
