package com.anju.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "file_upload_session")
public class FileUploadSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id", nullable = false, unique = true, length = 80)
    private String uploadId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "chunk_size")
    private Integer chunkSize;

    @Column(name = "status", nullable = false, length = 32)
    private String status;
}
