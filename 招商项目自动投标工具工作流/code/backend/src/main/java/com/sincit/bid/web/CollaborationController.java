package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.persistence.ProjectRepository;
import com.sincit.bid.service.CollaborationService;
import com.sincit.bid.service.FileStorageService;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 项目协作接口：附件、评论、关注、复制。
 */
@RestController
@RequestMapping("/projects/{id}")
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final ProjectRepository projectRepository;
    private final PermissionService permissionService;
    private final FileStorageService fileStorageService;

    public CollaborationController(CollaborationService collaborationService, ProjectRepository projectRepository,
                                   PermissionService permissionService, FileStorageService fileStorageService) {
        this.collaborationService = collaborationService;
        this.projectRepository = projectRepository;
        this.permissionService = permissionService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/attachments")
    public ApiResponse<ProjectDtos.Detail> upload(@PathVariable String id,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "category", required = false) String category) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(collaborationService.uploadAttachment(id, file, category, actor));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ApiResponse<ProjectDtos.Detail> deleteAttachment(@PathVariable String id,
                                                            @PathVariable String attachmentId) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(collaborationService.deleteAttachment(id, attachmentId, actor));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable String id, @PathVariable String attachmentId) {
        CurrentUser actor = UserContext.require();
        BidProject project = projectRepository.get(id);
        permissionService.assertView(actor, project);
        BidProject.Attachment target = null;
        if (project.getAttachments() != null) {
            for (BidProject.Attachment attachment : project.getAttachments()) {
                if (attachment.getId().equals(attachmentId)) {
                    target = attachment;
                    break;
                }
            }
        }
        if (target == null) {
            throw BusinessException.notFound("附件不存在");
        }
        Path path = fileStorageService.attachmentPath(project.getId(), target.getStorageName());
        String contentType = target.getContentType();
        if (contentType == null) {
            try {
                contentType = Files.probeContentType(path);
            } catch (Exception ignored) {
                contentType = "application/octet-stream";
            }
        }
        String filename;
        try {
            filename = URLEncoder.encode(target.getFilename(), "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            filename = "attachment";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(path));
    }

    @PostMapping("/comments")
    public ApiResponse<ProjectDtos.Detail> comment(@PathVariable String id,
                                                   @RequestBody Map<String, String> request) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(collaborationService.addComment(id, request.get("content"), actor));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<ProjectDtos.Detail> deleteComment(@PathVariable String id, @PathVariable String commentId) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(collaborationService.deleteComment(id, commentId, actor));
    }

    @PostMapping("/follow")
    public ApiResponse<Map<String, Object>> follow(@PathVariable String id) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(collaborationService.toggleFollow(id, actor));
    }

    @PostMapping("/duplicate")
    public ApiResponse<ProjectDtos.Detail> duplicate(@PathVariable String id) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(collaborationService.duplicate(id, actor));
    }
}
