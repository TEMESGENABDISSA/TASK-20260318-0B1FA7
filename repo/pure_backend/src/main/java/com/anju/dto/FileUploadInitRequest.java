package com.anju.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadInitRequest {
    private String fileName;
    private String contentHash;
    private Long fileSize;
    private Integer chunkSize;
    private Integer totalChunks;
    private String mimeType;
}
