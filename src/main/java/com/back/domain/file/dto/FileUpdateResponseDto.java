package com.back.domain.file.dto;

import lombok.Data;

@Data
public class FileUpdateResponseDto {
    private String publicURL;

    public FileUpdateResponseDto(String publicURL) {
        this.publicURL = publicURL;
    }
}
