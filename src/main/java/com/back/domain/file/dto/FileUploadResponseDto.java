package com.back.domain.file.dto;

import lombok.Data;

@Data
public class FileUploadResponseDto {
    private Long attachmentId;
    private String imageUrl;

    public FileUploadResponseDto(Long attachmentId, String imageUrl) {
        this.attachmentId = attachmentId;
        this.imageUrl = imageUrl;
    }
}