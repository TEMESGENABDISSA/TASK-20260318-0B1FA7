package com.anju.repository;

import com.anju.entity.IdempotencyRecord;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);

    void deleteByExpireAtBefore(LocalDateTime time);
}
