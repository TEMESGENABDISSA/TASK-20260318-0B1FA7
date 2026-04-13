package com.anju.repository;

import com.anju.entity.FileUploadSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUploadSessionRepository extends JpaRepository<FileUploadSession, Long> {
    Optional<FileUploadSession> findByUploadId(String uploadId);
    void deleteByUploadId(String uploadId);
}
