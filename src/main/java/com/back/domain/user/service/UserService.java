package com.back.domain.user.service;

import com.back.domain.user.dto.LoginRequest;
import com.back.domain.user.dto.UserRegisterRequest;
import com.back.domain.user.dto.UserResponse;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.entity.UserToken;
import com.back.domain.user.repository.UserProfileRepository;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.user.repository.UserTokenRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.CurrentUser;
import com.back.global.security.JwtTokenProvider;
import com.back.global.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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

        // TODO: 임시 로직 - 이메일 인증 기능 개발 전까지는 바로 ACTIVE 처리
        user.setUserStatus(UserStatus.ACTIVE);

        // 저장 (cascade로 Profile도 함께 저장됨)
        User saved = userRepository.save(user);

        // TODO: 이메일 인증 로직 추가 예정

        // UserResponse 변환 및 반환
        return UserResponse.from(saved, profile);
    }

    /**
     * 로그인 서비스
     * 1. 사용자 조회 및 비밀번호 검증
     * 2. 사용자 상태 검증 (PENDING, SUSPENDED, DELETED)
     * 3. Access/Refresh Token 발급
     * 4. Refresh Token을 HttpOnly 쿠키로, Access Token은 헤더로 설정
     * 5. UserResponse 반환
     */
    public UserResponse login(LoginRequest request, HttpServletResponse response) {
        // 사용자 조회
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 사용자 상태 검증
        switch (user.getUserStatus()) {
            case PENDING -> throw new CustomException(ErrorCode.USER_EMAIL_NOT_VERIFIED);
            case SUSPENDED -> throw new CustomException(ErrorCode.USER_SUSPENDED);
            case DELETED -> throw new CustomException(ErrorCode.USER_DELETED);
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // DB에 Refresh Token 저장
        UserToken userToken = new UserToken(
                user,
                refreshToken,
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationInSeconds())
        );
        userTokenRepository.save(userToken);

        // Refresh Token을 HttpOnly 쿠키로 설정
        CookieUtil.addCookie(
                response,
                "refreshToken",
                refreshToken,
                (int) jwtTokenProvider.getRefreshTokenExpirationInSeconds(),
                "/api/auth"
        );

        // Access Token을 응답 헤더에 설정
        response.setHeader("Authorization", "Bearer " + accessToken);

        // UserResponse 반환
        return UserResponse.from(user, user.getUserProfile());
    }

    /**
     * 로그아웃 서비스
     * 1. Refresh Token 검증 및 DB 삭제
     * 2. 쿠키 삭제
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 Refresh Token 추출
        String refreshToken = resolveRefreshToken(request);

        // 토큰 검증
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // DB에서 Refresh Token 삭제
        userTokenRepository.deleteByRefreshToken(refreshToken);

        // 쿠키 삭제
        CookieUtil.clearCookie(response, "refreshToken", "/api/auth");
    }

    /**
     * 로그아웃 서비스
     * 1. Refresh Token 검증 및 DB 삭제
     * 2. 쿠키 삭제
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 Refresh Token 추출
        String refreshToken = resolveRefreshToken(request);

        // Refresh Token 존재 여부 확인
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // DB에서 Refresh Token 삭제
        userTokenRepository.deleteByRefreshToken(refreshToken);

        // 쿠키 삭제
        CookieUtil.clearCookie(response, "refreshToken", "/api/auth");
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
        if (userProfileRepository.existsByNickname(request.nickname())) {
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

    /**
     * 쿠키에서 Refresh Token 추출
     */
    private String resolveRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
