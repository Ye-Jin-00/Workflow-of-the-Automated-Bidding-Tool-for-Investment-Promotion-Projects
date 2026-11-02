package com.sincit.bid.web.dto;

import com.sincit.bid.domain.AiDraft;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.ExtractionResult;
import com.sincit.bid.domain.MatchReport;
import com.sincit.bid.domain.PricingPlan;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.QualificationResult;
import com.sincit.bid.domain.ReviewRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 项目相关 DTO。字段与前端页面一一对应，避免直接暴露领域对象。
 */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    @Data
    public static class SaveRequest {
        private String name;
        private String clientName;
        private String carrierName;
        private String industry;
        private String source;
        private String department;
        /** 管理员可指定负责人，普通用户忽略该字段 */
        private String ownerId;
        private String deadline;
        private BigDecimal budgetAmount;
        private BigDecimal depositAmount;
        private String projectBrief;
        private String tenderText;
        private QualificationProfile profile;
        /** 更新时必填，用于乐观锁校验 */
        private Integer version;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private String id;
        private String code;
        private String name;
        private String clientName;
        private String carrierName;
        private String industry;
        private String status;
        private String statusLabel;
        private String statusType;
        private int stage;
        private String ownerId;
        private String ownerName;
        private String department;
        private LocalDateTime deadline;
        private BigDecimal budgetAmount;
        private BigDecimal bidAmount;
        private LocalDateTime updatedAt;
        private long daysToDeadline;
        private String riskLevel;
        private String riskText;
        private int reviewRound;
        private boolean following;
        private int attachmentCount;
        private int commentCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private String id;
        private String code;
        private String name;
        private String clientName;
        private String carrierName;
        private String industry;
        private String source;
        private String status;
        private String statusLabel;
        private String statusType;
        private int stage;
        private int version;
        private String ownerId;
        private String ownerName;
        private String department;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deadline;
        private long daysToDeadline;
        private BigDecimal budgetAmount;
        private BigDecimal bidAmount;
        private BigDecimal depositAmount;
        private String projectBrief;
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
        private List<AttachmentView> attachments = new ArrayList<AttachmentView>();
        @Builder.Default
        private List<CommentView> comments = new ArrayList<CommentView>();
        private BidProject.Submission submission;
        private BidProject.Archive archive;
        private String rejectionReason;
        private String lastReviewOpinion;
        private int reviewRound;
        private boolean following;
        private int attachmentCount;
        private int commentCount;
        private List<String> risks;
        private Map<String, Boolean> permissions;
        private List<String> allowedActions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentView {
        private String id;
        private String filename;
        private String category;
        private long size;
        private String contentType;
        private LocalDateTime uploadedAt;
        private String uploadedById;
        private String uploadedBy;
        private String downloadUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentView {
        private String id;
        private String authorId;
        private String authorName;
        private String avatarUrl;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    public static class QualificationRequest {
        private QualificationProfile profile;
        private Integer version;
    }

    @Data
    public static class ExtractRequest {
        private String tenderText;
        private Integer version;
    }

    @Data
    public static class MatchPriceRequest {
        private Integer personDays;
        /** 人天单价，单位：元 */
        private BigDecimal dailyRate;
        private Double subcontractRate;
        private Double travelRate;
        private Double taxRate;
        private Double discountRate;
        private Integer competitorCount;
        private Integer version;
    }

    @Data
    public static class AiDraftRequest {
        private String emphasis;
        private String tone;
        private Integer version;
    }

    @Data
    public static class DraftUpdateRequest {
        private List<AiDraft.Section> outline;
        private String technicalProposal;
        private String commercialResponse;
        private String qualificationResponse;
        private List<String> riskNotes;
        private Integer version;
    }

    @Data
    public static class ReviewRequest {
        /** APPROVE / RETURN */
        private String decision;
        private String opinion;
        private Integer version;
    }

    @Data
    public static class SubmitRequest {
        private String method;
        private String referenceNo;
        private String note;
        private Integer version;
    }

    @Data
    public static class ArchiveRequest {
        private String archiveNo;
        private Integer retentionYears;
        private String note;
        private Integer version;
    }
}
