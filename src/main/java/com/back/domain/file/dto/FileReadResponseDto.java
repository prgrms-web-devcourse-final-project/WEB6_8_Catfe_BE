package com.back.domain.file.dto;

import lombok.Data;

@Data
public class FileReadResponseDto {
    private String publicURL;

    public FileReadResponseDto(String publicURL) {
        this.publicURL = publicURL;
    }
}
