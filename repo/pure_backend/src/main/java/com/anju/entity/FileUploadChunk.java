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
@Table(name = "file_upload_chunk")
public class FileUploadChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id", nullable = false, length = 80)
    private String uploadId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
}
