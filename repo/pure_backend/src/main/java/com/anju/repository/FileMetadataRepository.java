package com.anju.repository;

import com.anju.entity.FileMetadata;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findByContentHashAndDeletedFalse(String contentHash);
    List<FileMetadata> findByDeletedTrueAndDeleteExpireAtBefore(LocalDateTime now);
    List<FileMetadata> findByDeletedTrue();
}
