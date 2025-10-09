package com.back.domain.file.dto;

import lombok.Data;

@Data
public class FileUpdateResponseDto {
    private String imageURL;

    public FileUpdateResponseDto(String imageURL) {
        this.imageURL = imageURL;
    }
}
