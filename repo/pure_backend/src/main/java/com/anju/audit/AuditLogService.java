package com.anju.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void logAction(String action, String entityType, String entityId, Object beforeObj, Object afterObj) {
        Map<String, Object> before = AuditSnapshotUtil.toMap(objectMapper, beforeObj);
        Map<String, Object> after = AuditSnapshotUtil.toMap(objectMapper, afterObj);
        Map<String, Map<String, Object>> changes = AuditSnapshotUtil.diff(before, after);

        AuditLog log = new AuditLog();
        log.setOperator(resolveOperator());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOccurredAt(LocalDateTime.now());
        log.setBeforeJson(toJson(before));
        log.setAfterJson(toJson(after));
        log.setFieldChangesJson(toJson(changes));
        auditLogRepository.save(log);
    }

    private String resolveOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
