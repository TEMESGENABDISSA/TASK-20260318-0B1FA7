package com.anju.service;

import com.anju.common.BusinessException;
import com.anju.entity.IdempotencyRecord;
import com.anju.repository.IdempotencyRecordRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final int TTL_HOURS = 48;
    private final IdempotencyRecordRepository repository;

    @Transactional
    public void verifyOrStore(String scope, String key, String fingerprint) {
        if (key == null || key.isBlank()) {
            throw new BusinessException("Idempotency-Key header is required");
        }
        LocalDateTime now = LocalDateTime.now();
        var existing = repository.findByScopeAndIdempotencyKey(scope, key);
        if (existing.isEmpty()) {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setScope(scope);
            record.setIdempotencyKey(key);
            record.setFingerprint(fingerprint);
            record.setExpireAt(now.plusHours(TTL_HOURS));
            repository.save(record);
            return;
        }

        IdempotencyRecord record = existing.get();
        if (record.getExpireAt().isBefore(now)) {
            record.setFingerprint(fingerprint);
            record.setExpireAt(now.plusHours(TTL_HOURS));
            repository.save(record);
            return;
        }
        if (!record.getFingerprint().equals(fingerprint)) {
            throw new BusinessException("Idempotency key conflict with different payload");
        }
        throw new BusinessException("Duplicate request rejected by idempotency rule");
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void cleanupExpired() {
        repository.deleteByExpireAtBefore(LocalDateTime.now());
    }
}
