package com.back.domain.user.service;

import com.back.domain.user.dto.LoginRequest;
import com.back.domain.user.dto.LoginResponse;
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
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.util.CookieUtil;
import com.back.global.util.PasswordValidator;
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
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserTokenRepository userTokenRepository;
    private final EmailService emailService;
    private final TokenService tokenService;
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
        PasswordValidator.validate(request.password());

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

        // 이메일 인증 토큰 생성 및 이메일 발송
         String emailToken = tokenService.createEmailVerificationToken(saved.getId());
         emailService.sendVerificationEmail(saved.getEmail(), emailToken);

        // UserResponse 변환 및 반환
        return UserResponse.from(saved);
    }

    /**
     * 이메일 인증 서비스
     * 1. 토큰 존재 여부 확인
     * 2. 사용자 조회 및 활성화 (PENDING -> ACTIVE)
     * 5. 토큰 삭제
     * 6. UserResponse 반환
     */
    public UserResponse verifyEmail(String token) {

        // 토큰 존재 여부 확인
        if (token == null || token.isEmpty()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // 토큰으로 사용자 ID 조회
        Long userId = tokenService.getUserIdByEmailVerificationToken(token);
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_TOKEN);
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자 상태 검증
        if (user.getUserStatus() == UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ALREADY_VERIFIED);
        }

        // 사용자 상태를 ACTIVE로 변경
        user.setUserStatus(UserStatus.ACTIVE);

        // 토큰 삭제 (재사용 방지)
        tokenService.deleteEmailVerificationToken(token);

        // UserResponse 반환
        return UserResponse.from(user);
    }

    /**
     * 로그인 서비스
     * 1. 사용자 조회 및 비밀번호 검증
     * 2. 사용자 상태 검증 (PENDING, SUSPENDED, DELETED)
     * 3. Access/Refresh Token 발급
     * 4. Refresh Token을 HttpOnly 쿠키로, Access Token은 헤더로 설정
     * 5. LoginResponse 반환
     */
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
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
                "/",
                true
        );


        // LoginResponse 반환
        return new LoginResponse(
                accessToken,
                UserResponse.from(user)
        );
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
        jwtTokenProvider.validateRefreshToken(refreshToken);

        // DB에서 Refresh Token 삭제
        userTokenRepository.deleteByRefreshToken(refreshToken);

        // 쿠키 삭제
        CookieUtil.clearCookie(
                response,
                "refreshToken",
                "/",
                true
        );
    }

    /**
     * 토큰 재발급 서비스
     * 1. 쿠키에서 Refresh Token 추출
     * 2. Refresh Token 검증 (만료/위조 확인)
     * 3. DB에 저장된 Refresh Token 여부 확인
     * 4. 새 Access Token 발급
     */
    public String refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Refresh Token 검증
        String refreshToken = resolveRefreshToken(request);

        // Refresh Token 존재 여부 확인
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // Refresh Token 검증
        jwtTokenProvider.validateRefreshToken(refreshToken);

        // DB에서 Refresh Token 조회
        UserToken userToken = userTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 사용자 정보 조회
        User user = userToken.getUser();

        // 새로운 Access Token 발급
        return jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
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
