package com.sincit.bid.web;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.User;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.service.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 头像读取接口。头像通过 public URL 直接展示，不携带令牌，避免 img 标签无法设置 Header。
 */
@RestController
public class AvatarController {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public AvatarController(UserRepository userRepository, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/avatars/{userId}")
    public ResponseEntity<Resource> avatar(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        Path path = fileStorageService.avatarPath(user.getAvatar());
        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (Exception e) {
            contentType = null;
        }
        MediaType mediaType = contentType == null
                ? MediaType.IMAGE_PNG : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(new FileSystemResource(path));
    }
}
