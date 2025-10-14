package com.back.domain.user.auth.dto;

import com.back.domain.user.common.enums.Role;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.enums.UserStatus;

import java.time.LocalDateTime;

/**
 * 사용자 응답을 나타내는 DTO
 *
 * @param userId    사용자의 고유 ID
 * @param username  사용자의 로그인 id
 * @param email     사용자의 이메일 주소
 * @param nickname  사용자의 별명
 * @param role      사용자의 역할 (예: USER, ADMIN)
 * @param status    사용자의 상태 (예: ACTIVE, PENDING)
 * @param createdAt 사용자가 생성된 날짜 및 시간
 */
public record UserResponse(
        Long userId,
        String username,
        String email,
        String nickname,
        Role role,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserProfile().getNickname(),
                user.getRole(),
                user.getUserStatus(),
                user.getCreatedAt()
        );
    }
}
