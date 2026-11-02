package com.sincit.bid.service;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件存储：头像与项目附件。
 * 生产环境可替换为对象存储，接口保持不变；storageName 由服务端生成，避免路径穿越。
 */
@Service
public class FileStorageService {

    private static final long AVATAR_MAX = 3 * 1024 * 1024L;
    private static final long ATTACHMENT_MAX = 20 * 1024 * 1024L;
    private static final Set<String> AVATAR_EXT = new HashSet<String>(Arrays.asList("jpg", "jpeg", "png", "webp", "gif"));
    private static final Set<String> FILE_EXT = new HashSet<String>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "png", "jpg", "jpeg"));

    private final Path avatarDir;
    private final Path uploadDir;

    public FileStorageService(AppProperties properties) {
        Path base = Paths.get(properties.getDataDir()).toAbsolutePath().normalize();
        this.avatarDir = base.resolve("avatars");
        this.uploadDir = base.resolve("uploads");
        try {
            Files.createDirectories(avatarDir);
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("初始化文件存储目录失败", e);
        }
    }

    public String saveAvatar(String userId, MultipartFile file) {
        validate(file, AVATAR_MAX, "头像");
        String ext = extension(file.getOriginalFilename());
        if (!AVATAR_EXT.contains(ext)) {
            throw BusinessException.invalid("头像仅支持 JPG、PNG、WEBP、GIF 格式");
        }
        String filename = userId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "." + ext;
        try {
            file.transferTo(avatarDir.resolve(filename).toFile());
            return filename;
        } catch (IOException e) {
            throw new IllegalStateException("保存头像失败", e);
        }
    }

    public Path avatarPath(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw BusinessException.notFound("头像不存在");
        }
        Path path = avatarDir.resolve(filename).normalize();
        if (!path.startsWith(avatarDir) || !Files.exists(path)) {
            throw BusinessException.notFound("头像不存在");
        }
        return path;
    }

    public void deleteAvatar(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        try {
            Path path = avatarDir.resolve(filename).normalize();
            if (path.startsWith(avatarDir)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // 删除失败不影响主流程
        }
    }

    public StoredFile saveAttachment(String projectId, MultipartFile file) {
        validate(file, ATTACHMENT_MAX, "附件");
        String original = safeOriginalName(file.getOriginalFilename());
        String ext = extension(original);
        if (!FILE_EXT.contains(ext)) {
            throw BusinessException.invalid("不支持的附件格式，请上传 PDF、Word、Excel、PPT、TXT、ZIP 或图片");
        }
        Path projectDir = uploadDir.resolve(projectId).normalize();
        if (!projectDir.startsWith(uploadDir)) {
            throw BusinessException.invalid("项目标识不合法");
        }
        try {
            Files.createDirectories(projectDir);
            String storageName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(projectDir.resolve(storageName).toFile());
            return new StoredFile(storageName, original, file.getSize(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("保存附件失败", e);
        }
    }

    public Path attachmentPath(String projectId, String storageName) {
        Path projectDir = uploadDir.resolve(projectId).normalize();
        Path path = projectDir.resolve(storageName).normalize();
        if (!path.startsWith(projectDir) || !Files.exists(path)) {
            throw BusinessException.notFound("附件不存在");
        }
        return path;
    }

    public void deleteAttachment(String projectId, String storageName) {
        try {
            Path projectDir = uploadDir.resolve(projectId).normalize();
            Path path = projectDir.resolve(storageName).normalize();
            if (path.startsWith(projectDir)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // 删除失败不影响主流程
        }
    }

    private void validate(MultipartFile file, long max, String label) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.invalid("请选择要上传的" + label);
        }
        if (file.getSize() > max) {
            throw BusinessException.invalid(label + "大小不能超过 " + (max / 1024 / 1024) + "MB");
        }
    }

    private String safeOriginalName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "file";
        }
        String value = name.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1);
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) {
            throw BusinessException.invalid("文件缺少扩展名");
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public static class StoredFile {
        private final String storageName;
        private final String originalName;
        private final long size;
        private final String contentType;

        public StoredFile(String storageName, String originalName, long size, String contentType) {
            this.storageName = storageName;
            this.originalName = originalName;
            this.size = size;
            this.contentType = contentType;
        }

        public String getStorageName() {
            return storageName;
        }

        public String getOriginalName() {
            return originalName;
        }

        public long getSize() {
            return size;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
