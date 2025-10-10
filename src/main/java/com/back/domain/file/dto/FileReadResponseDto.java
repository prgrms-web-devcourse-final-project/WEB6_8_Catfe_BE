package com.back.domain.file.dto;

import lombok.Data;

@Data
public class FileReadResponseDto {
    private String imageUrl;

    public FileReadResponseDto(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
