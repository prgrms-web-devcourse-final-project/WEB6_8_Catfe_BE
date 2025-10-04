package com.back.domain.board.comment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 댓글 작성 및 수정을 위한 요청 DTO
 *
 * @param content   댓글 내용
 */
public record CommentRequest(
        @NotBlank String content
) {
}
