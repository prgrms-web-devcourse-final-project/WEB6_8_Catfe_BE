package com.back.domain.notification.dto;

import com.back.domain.user.entity.User;

public record ActorDto(
        Long userId,
        String username,
        String profileImageUrl
) {
    public static ActorDto from(User user) {
        if (user == null) return null;
        return new ActorDto(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
