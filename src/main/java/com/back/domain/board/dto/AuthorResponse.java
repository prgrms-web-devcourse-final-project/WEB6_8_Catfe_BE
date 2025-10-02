package com.back.domain.board.dto;

import com.back.domain.user.entity.User;

/**
 * 작성자 응답 DTO
 *
 * @param id       작성자 ID
 * @param nickname 작성자 닉네임
 */
public record AuthorResponse(
        Long id,
        String nickname
) {
    public static AuthorResponse from(User user) {
        return new AuthorResponse(
                user.getId(),
                user.getUserProfile().getNickname()
        );
    }
}