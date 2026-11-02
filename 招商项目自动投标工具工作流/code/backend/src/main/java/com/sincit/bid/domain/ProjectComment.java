package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 项目评论。附件与评论都挂在项目聚合根上，详情页一次读取即可展示协作上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectComment {

    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String content;
    private LocalDateTime createdAt;
}
