package com.back.domain.user.service;

import com.back.domain.user.dto.UserRegisterRequest;
import com.back.domain.user.dto.UserResponse;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 서비스
     * 1. 중복 검사 (username, email, nickname)
     * 2. 비밀번호 정책 검증
     * 3. User + UserProfile 생성 및 연관관계 설정
     * 4. 저장 후 UserResponse 변환
     */
    public UserResponse register(UserRegisterRequest request) {

        // 중복 검사 (username, email, nickname)
        validateDuplicate(request);

        // 비밀번호 정책 검증
        validatePasswordPolicy(request.password());

        // User 엔티티 생성 (기본 Role.USER, Status.PENDING)
        User user = User.createUser(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        // UserProfile 엔티티 생성
        UserProfile profile = new UserProfile(
                user,
                request.nickname(),
                null,
                null,
                null,
                0
        );

        // 연관관계 설정
        user.setUserProfile(profile);

        // 저장 (cascade로 Profile도 함께 저장됨)
        User saved = userRepository.save(user);

        // UserResponse 변환 및 반환
        return UserResponse.from(saved, profile);
    }

    /**
     * 회원가입 시 중복 검증
     * - username, email, nickname
     */
    private void validateDuplicate(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new CustomException(ErrorCode.USERNAME_DUPLICATED);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
        }
    }

    /**
     * 비밀번호 정책 검증
     * - 최소 8자 이상
     * - 숫자 및 특수문자 반드시 포함
     */
    private void validatePasswordPolicy(String password) {
        String regex = "^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$";
        if (!password.matches(regex)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
