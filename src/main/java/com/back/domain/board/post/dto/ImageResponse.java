package com.back.domain.board.post.dto;

import com.back.domain.file.entity.FileAttachment;

/**
 * 이미지 응답 DTO
 *
 * @param id  이미지 ID
 * @param url 이미지 URL
 */
public record ImageResponse(
        Long id,
        String url
) {
    public static ImageResponse from(FileAttachment fileAttachment) {
        return new ImageResponse(
                fileAttachment.getId(),
                fileAttachment.getPublicURL()
        );
    }
}
