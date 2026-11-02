package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人工复核记录，支持多轮退回与重新提交，全部保留。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecord {

    private String id;
    private int round;
    private String reviewerId;
    private String reviewerName;
    /** APPROVE / RETURN */
    private ReviewDecision decision;
    private String opinion;
    private LocalDateTime createdAt;

    public enum ReviewDecision {
        APPROVE("通过"),
        RETURN("退回修改");

        private final String label;

        ReviewDecision(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
