package com.anju.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileUploadInitResponse {
    private String uploadId;
    private boolean instantUpload;
    private Long fileId;
    private List<Integer> uploadedChunks;
}
