package com.anju.repository;

import com.anju.entity.FileUploadChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUploadChunkRepository extends JpaRepository<FileUploadChunk, Long> {
    boolean existsByUploadIdAndChunkIndex(String uploadId, Integer chunkIndex);
    long countByUploadId(String uploadId);
    List<FileUploadChunk> findByUploadId(String uploadId);
}
