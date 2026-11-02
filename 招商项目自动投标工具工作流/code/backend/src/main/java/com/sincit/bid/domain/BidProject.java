package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 招商投标项目聚合根。工作流产物（预审、抽取、测算、初稿、复核）直接随项目保存，
 * 保证一次读取即可完整还原业务上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidProject {

    private String id;
    private String code;
    private String name;
    /** 招标人/委托方 */
    private String clientName;
    /** 项目载体，如泛悦城市广场 T2 */
    private String carrierName;
    private String industry;
    private String source;
    private String ownerId;
    private String ownerName;
    private String department;
    private BidStatus status;
    /** 软删除标记：删除后进入回收站，30 天后自动清理 */
    @Builder.Default
    private boolean deleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private String deletedById;
    private String deleteReason;
    private BidStatus statusBeforeDelete;
    /** 乐观锁版本号，防止多人同时推进流程造成覆盖 */
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    /** 投标截止时间 */
    private LocalDateTime deadline;
    /** 招标预算/最高限价，单位：万元 */
    private BigDecimal budgetAmount;
    /** 投标报价，单位：万元 */
    private BigDecimal bidAmount;
    /** 投标保证金，单位：万元 */
    private BigDecimal depositAmount;
    private String projectBrief;
    /** 招标文件正文（演示版以文本粘贴代替 PDF/OCR 解析） */
    private String tenderText;

    private QualificationProfile profile;
    private QualificationResult qualificationResult;
    private ExtractionResult extraction;
    private MatchReport matchReport;
    private PricingPlan pricingPlan;
    private AiDraft aiDraft;

    @Builder.Default
    private List<ReviewRecord> reviews = new ArrayList<ReviewRecord>();
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<Attachment>();
    @Builder.Default
    private List<ProjectComment> comments = new ArrayList<ProjectComment>();
    /** 关注人：项目状态变更、截止提醒、评论与附件会通知他们 */
    @Builder.Default
    private List<String> watcherIds = new ArrayList<String>();

    private Submission submission;
    private Archive archive;
    private String rejectionReason;
    private String lastReviewOpinion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String id;
        private String filename;
        /** 招标文件 / 资格材料 / 标书文件 / 回执凭证 / 其他 */
        private String category;
        private long size;
        private String contentType;
        /** 服务器存储文件名，不直接暴露给前端 */
        private String storageName;
        private String uploadedById;
        private LocalDateTime uploadedAt;
        private String uploadedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Submission {
        private LocalDateTime submittedAt;
        private String method;
        private String referenceNo;
        private String operatorName;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Archive {
        private LocalDateTime archivedAt;
        private String archiveNo;
        private Integer retentionYears;
        private String operatorName;
        private String note;
    }
}
