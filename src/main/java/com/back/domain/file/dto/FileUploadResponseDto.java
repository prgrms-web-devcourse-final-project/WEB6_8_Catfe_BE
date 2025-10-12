package com.back.domain.file.dto;

import lombok.Data;

@Data
public class FileUploadResponseDto {
    private Long attachmentId;
    private String publicURL;

    public FileUploadResponseDto(Long attachmentId, String publicURL) {
        this.attachmentId = attachmentId;
        this.publicURL = publicURL;
    }
}