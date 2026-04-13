package com.anju.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_no", nullable = false, unique = true, length = 64)
    private String fileNo;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Column(name = "uploaded_chunks", nullable = false)
    private Integer uploadedChunks;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "delete_expire_at")
    private LocalDateTime deleteExpireAt;
}
