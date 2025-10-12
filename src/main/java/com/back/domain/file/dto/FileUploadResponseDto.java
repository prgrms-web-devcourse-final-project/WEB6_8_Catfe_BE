package com.back.domain.file.dto;

import lombok.Data;

@Data
public class FileUploadResponseDto {
    private String imageUrl;

    public FileUploadResponseDto(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}