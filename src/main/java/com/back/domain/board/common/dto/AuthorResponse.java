package com.back.domain.board.common.dto;

import com.back.domain.user.common.entity.User;
import com.querydsl.core.annotations.QueryProjection;

/**
 * 작성자 응답 DTO
 *
 * @param id              작성자 ID
 * @param nickname        작성자 닉네임
 * @param profileImageUrl 작성자 프로필 이미지
 */
public record AuthorResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {
    @QueryProjection
    public AuthorResponse {}

    public static AuthorResponse from(User user) {
        return new AuthorResponse(
                user.getId(),
                user.getUserProfile().getNickname(),
                user.getProfileImageUrl()
        );
    }
}