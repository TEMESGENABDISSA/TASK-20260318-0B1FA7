package com.anju.controller;

import com.anju.audit.AuditLog;
import com.anju.audit.AuditLogRepository;
import com.anju.common.ApiResponse;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        int p = Math.max(0, page - 1);
        int s = Math.min(Math.max(1, size), 200);
        var pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "occurredAt"));
        var result = auditLogRepository.findAll(pageable);
        return ApiResponse.ok(Map.of(
                "items", result.getContent(),
                "page", page,
                "size", s,
                "total", result.getTotalElements()
        ));
    }

    @GetMapping("/logs/{logId}")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ApiResponse<AuditLog> get(@PathVariable Long logId) {
        AuditLog log = auditLogRepository.findById(logId)
                .orElseThrow(() -> new com.anju.common.BusinessException("audit log not found"));
        return ApiResponse.ok(log);
    }

    @PostMapping("/logs:export")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ApiResponse<Map<String, Object>> export(@RequestBody(required = false) Map<String, Object> request) {
        // Static-only export placeholder: returns an export task reference.
        // A real implementation should generate CSV and store as FileMetadata.
        return ApiResponse.ok(Map.of(
                "exportId", "AUDIT-EXPORT-" + System.currentTimeMillis(),
                "createdAt", LocalDateTime.now().toString(),
                "status", "CREATED"
        ));
    }

    @GetMapping("/logs/export.csv")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(defaultValue = "200") int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 2000);
        List<AuditLog> logs = auditLogRepository.findAll(PageRequest.of(
                0, safeLimit, Sort.by(Sort.Direction.DESC, "occurredAt")
        )).getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("id,operator,action,entityType,entityId,occurredAt\n");
        for (AuditLog l : logs) {
            sb.append(l.getId()).append(',')
              .append(csv(l.getOperator())).append(',')
              .append(csv(l.getAction())).append(',')
              .append(csv(l.getEntityType())).append(',')
              .append(csv(l.getEntityId())).append(',')
              .append(l.getOccurredAt())
              .append('\n');
        }

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header("Content-Disposition", "attachment; filename=\"audit-export.csv\"")
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csv(String v) {
        if (v == null) {
            return "";
        }
        String escaped = v.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

