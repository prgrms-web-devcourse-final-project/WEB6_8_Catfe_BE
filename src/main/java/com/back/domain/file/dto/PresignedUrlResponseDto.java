package com.back.domain.file.dto;

import lombok.Data;

@Data
public class PresignedUrlResponseDto {
    private String uploadUrl;
    private String objectUrl;

    public PresignedUrlResponseDto(String uploadUrl, String objectUrl) {
        this.uploadUrl = uploadUrl;
        this.objectUrl = objectUrl;
    }
}
