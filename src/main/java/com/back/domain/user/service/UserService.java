package com.back.domain.user.service;

import com.back.domain.user.dto.UpdateUserProfileRequest;
import com.back.domain.user.dto.UserDetailResponse;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserProfileRepository;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * 사용자 정보 조회 서비스
     * 1. 사용자 조회 및 상태 검증
     * 2. UserDetailResponse 변환 및 반환
     */
    public UserDetailResponse getUserInfo(Long userId) {

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
}
