package com.sincit.bid.web;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.service.ImportService;
import com.sincit.bid.service.ProjectBatchImportService;
import com.sincit.bid.web.dto.BatchDtos;
import com.sincit.bid.web.dto.ImportDtos;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 招标文件导入识别与批量导入模板。
 */
@RestController
@RequestMapping("/projects/import")
public class ImportController {

    private final ImportService importService;
    private final ProjectBatchImportService batchImportService;

    public ImportController(ImportService importService, ProjectBatchImportService batchImportService) {
        this.importService = importService;
        this.batchImportService = batchImportService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        try (InputStream input = new ClassPathResource("templates/project-import-template.xlsx").getInputStream()) {
            byte[] bytes = StreamUtils.copyToByteArray(input);
            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename("项目批量导入模板.xlsx", StandardCharsets.UTF_8).build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("导入模板读取失败", e);
        }
    }

    @PostMapping("/batch")
    public ApiResponse<BatchDtos.BatchImportResult> batch(@RequestParam("file") MultipartFile file) {
        CurrentUser actor = UserContext.require();
        return ApiResponse.ok(batchImportService.importFile(file, actor));
    }

    @PostMapping("/parse")
    public ApiResponse<ImportDtos.ImportResult> parse(@RequestBody ImportDtos.ParseRequest request) {
        return ApiResponse.ok(importService.parseText(request.getText(), "粘贴文本"));
    }

    @PostMapping("/file")
    public ApiResponse<ImportDtos.ImportResult> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(importService.parseFile(file));
    }
}
