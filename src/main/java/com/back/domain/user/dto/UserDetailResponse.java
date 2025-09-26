package com.back.domain.user.dto;

import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 상세 응답을 나타내는 DTO
 *
 * @param userId     사용자의 고유 ID
 * @param username   사용자의 로그인 id
 * @param email      사용자의 이메일 주소
 * @param role       사용자의 역할 (예: USER, ADMIN)
 * @param status     사용자의 상태 (예: ACTIVE, PENDING)
 * @param provider   사용자의 인증 제공자 (예: GOOGLE, FACEBOOK)
 * @param providerId 사용자의 인증 제공자 ID
 * @param profile    사용자의 프로필 정보
 * @param createdAt  사용자 정보가 생성된 날짜 및 시간
 * @param updatedAt  사용자 정보가 마지막으로 업데이트된 날짜 및 시간
 */
public record UserDetailResponse(
        Long userId,
        String username,
        String email,
        Role role,
        UserStatus status,
        String provider,
        String providerId,
        ProfileResponse profile,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getUserStatus(),
                user.getProvider(),
                user.getProviderId(),
                ProfileResponse.from(user.getUserProfile()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * 사용자 프로필 정보를 나타내는 DTO
     *
     * @param nickname        사용자의 별명
     * @param profileImageUrl 사용자의 프로필 이미지 URL
     * @param bio             사용자의 자기소개
     * @param birthDate       사용자의 생년월일
     * @param point           사용자의 포인트
     */
    public record ProfileResponse(
            String nickname,
            String profileImageUrl,
            String bio,
            LocalDate birthDate,
            int point
    ) {
        public static ProfileResponse from(UserProfile profile) {
            if (profile == null) return null;
            return new ProfileResponse(
                    profile.getNickname(),
                    profile.getProfileImageUrl(),
                    profile.getBio(),
                    profile.getBirthDate(),
                    profile.getPoint()
            );
        }
    }
}
