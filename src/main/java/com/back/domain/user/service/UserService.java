package com.back.domain.user.service;

import com.back.domain.user.dto.ChangePasswordRequest;
import com.back.domain.user.dto.UpdateUserProfileRequest;
import com.back.domain.user.dto.UserDetailResponse;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserProfileRepository;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 정보 조회 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. UserDetailResponse 변환 및 반환
     */
    public UserDetailResponse getUserInfo(Long userId) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // UserDetailResponse로 변환하여 반환
        return UserDetailResponse.from(user);
    }

    /**
     * 사용자 프로필 수정 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. 닉네임 중복 검사
     * 3. UserProfile 업데이트
     * 4. UserDetailResponse 변환 및 반환
     */
    public UserDetailResponse updateUserProfile(Long userId, UpdateUserProfileRequest request) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // 닉네임 중복 검사 (본인 제외)
        if (userProfileRepository.existsByNicknameAndUserIdNot(request.nickname(), userId)) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // UserProfile 업데이트
        UserProfile profile = user.getUserProfile();
        profile.setNickname(request.nickname());
        profile.setProfileImageUrl(request.profileImageUrl());
        profile.setBio(request.bio());
        profile.setBirthDate(request.birthDate());

        // UserDetailResponse로 변환하여 반환
        return UserDetailResponse.from(user);
    }

    /**
     * 비밀번호 변경 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. 현재 비밀번호 검증
     * 3. 새 비밀번호 정책 검증
     * 4. 비밀번호 변경
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // 소셜 로그인 사용자는 비밀번호 변경 불가
        if (user.getProvider() != null) {
            throw new CustomException(ErrorCode.SOCIAL_PASSWORD_CHANGE_FORBIDDEN);
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 새 비밀번호 정책 검증
        PasswordValidator.validate(request.newPassword());

        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 사용자 탈퇴 서비스 (soft delete)
     * 1. 사용자 조회 및 상태 검증
     * 2. UserStatus를 DELETED로 변경
     */
    public void deleteUser(Long userId) {

        // 사용자 조회 및 상태 검증
        User user = getValidUser(userId);

        // 상태 변경 (soft delete)
        user.setUserStatus(UserStatus.DELETED);

        // 식별 정보 변경 (username, email, provider, providerId)
        user.setUsername("deleted_" + user.getUsername());
        user.setEmail("deleted_" + user.getEmail());
        user.setProvider("deleted_" + user.getProvider());
        user.setProviderId("deleted_" + user.getProviderId());

        // 개인정보 마스킹
        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            profile.setNickname("탈퇴한 회원");
            profile.setProfileImageUrl(null);
            profile.setBio(null);
            profile.setBirthDate(null);
        }
    }

    /**
     * 유효한 사용자 조회 및 상태 검증
     *
     * @param userId 사용자 ID
     * @return user  조회된 사용자 엔티티
     */
    private User getValidUser(Long userId) {

        // userId로 User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // UserStatus가 DELETED, SUSPENDED면 예외 처리
        if (user.getUserStatus() == UserStatus.DELETED) {
            throw new CustomException(ErrorCode.USER_DELETED);
        }
        if (user.getUserStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }

        return user;
    }
}
