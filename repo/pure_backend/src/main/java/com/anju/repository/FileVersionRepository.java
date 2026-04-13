package com.anju.repository;

import com.anju.entity.FileVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByFileIdOrderByVersionNoAsc(Long fileId);
    Optional<FileVersion> findByFileIdAndVersionNo(Long fileId, Integer versionNo);
    void deleteByFileId(Long fileId);
}
