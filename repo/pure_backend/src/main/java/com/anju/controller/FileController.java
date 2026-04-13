package com.anju.controller;

import com.anju.common.ApiResponse;
import com.anju.dto.FileUploadInitRequest;
import com.anju.dto.FileUploadInitResponse;
import com.anju.security.RequireSecondaryPassword;
import com.anju.service.FileService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/uploads:init")
    @PreAuthorize("hasAnyRole('OPERATOR','DISPATCHER','ADMIN')")
    public ApiResponse<FileUploadInitResponse> initUpload(@RequestBody FileUploadInitRequest request) {
        return ApiResponse.ok(fileService.initUpload(request));
    }

    @PostMapping("/uploads/{uploadId}/chunks/{chunkIndex}")
    @PreAuthorize("hasAnyRole('OPERATOR','DISPATCHER','ADMIN')")
    public ApiResponse<Map<String, Object>> uploadChunk(@PathVariable String uploadId,
                                                        @PathVariable Integer chunkIndex,
                                                        @RequestParam(required = false, defaultValue = "524288") Long chunkBytes,
                                                        @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(fileService.uploadChunk(uploadId, chunkIndex, chunkBytes, file));
    }

    @PostMapping("/uploads/{uploadId}:complete")
    @PreAuthorize("hasAnyRole('OPERATOR','DISPATCHER','ADMIN')")
    public ApiResponse<Map<String, Object>> completeUpload(@PathVariable String uploadId) {
        return ApiResponse.ok(fileService.completeUpload(uploadId));
    }

    @GetMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('OPERATOR','REVIEWER','DISPATCHER','FINANCE','ADMIN')")
    public ApiResponse<Map<String, Object>> getFile(@PathVariable Long fileId) {
        return ApiResponse.ok(fileService.getFile(fileId));
    }

    @PostMapping("/{fileId}:new-version")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<Map<String, Object>> newVersion(@PathVariable Long fileId,
                                                       @RequestParam String uploadId,
                                                       @RequestParam(required = false) String changeNote) {
        return ApiResponse.ok(fileService.newVersionFromUpload(fileId, uploadId, changeNote));
    }

    @PostMapping("/{fileId}:rollback")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<Map<String, Object>> rollback(@PathVariable Long fileId, @RequestParam Integer targetVersion) {
        return ApiResponse.ok(fileService.rollback(fileId, targetVersion));
    }

    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<Map<String, Object>> recycle(@PathVariable Long fileId) {
        return ApiResponse.ok(fileService.recycle(fileId));
    }

    @PostMapping("/{fileId}:restore")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<Map<String, Object>> restore(@PathVariable Long fileId) {
        return ApiResponse.ok(fileService.restore(fileId));
    }

    @GetMapping("/recycle-bin")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ApiResponse<List<Map<String, Object>>> listRecycleBin() {
        return ApiResponse.ok(fileService.listRecycleBin());
    }

    @GetMapping("/{fileId}/preview")
    @PreAuthorize("hasAnyRole('OPERATOR','REVIEWER','DISPATCHER','FINANCE','ADMIN')")
    public ApiResponse<Map<String, Object>> preview(@PathVariable Long fileId) {
        return ApiResponse.ok(fileService.preview(fileId));
    }

    @GetMapping("/{fileId}/preview-content")
    @PreAuthorize("hasAnyRole('OPERATOR','REVIEWER','DISPATCHER','FINANCE','ADMIN')")
    public ApiResponse<Map<String, Object>> previewContent(@PathVariable Long fileId) {
        return ApiResponse.ok(fileService.previewContent(fileId));
    }

    @GetMapping("/{fileId}/content")
    @PreAuthorize("hasAnyRole('OPERATOR','REVIEWER','DISPATCHER','FINANCE','ADMIN')")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        return fileService.downloadCurrentVersion(fileId);
    }

    @DeleteMapping("/{fileId}:purge")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @RequireSecondaryPassword
    public ApiResponse<Map<String, Object>> purge(@PathVariable Long fileId) {
        return ApiResponse.ok(fileService.purge(fileId));
    }
}
