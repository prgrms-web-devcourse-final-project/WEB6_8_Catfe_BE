package com.back.domain.user.auth.service;

import com.back.domain.notification.service.NotificationSettingService;
import com.back.domain.user.auth.dto.LoginRequest;
import com.back.domain.user.auth.dto.LoginResponse;
import com.back.domain.user.auth.dto.UserRegisterRequest;
import com.back.domain.user.auth.dto.UserResponse;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.entity.UserToken;
import com.back.domain.user.common.repository.UserProfileRepository;
import com.back.domain.user.common.repository.UserRepository;
import com.back.domain.user.common.repository.UserTokenRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.domain.user.common.util.CookieUtil;
import com.back.domain.user.common.util.PasswordValidator;
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
    private final NotificationSettingService notificationSettingService;

    /**
     * 회원가입 서비스
     * 1. 중복 검사 (username, email, nickname)
     * 2. 비밀번호 정책 검증
     * 3. User + UserProfile 생성 및 연관관계 설정
     * 4. 저장 후 UserResponse 변환
     * 5. 알림 설정 초기화
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

        // 알림 설정 초기화 (모든 알림 타입 기본 활성화)
        notificationSettingService.initializeDefaultSettings(saved.getId());

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
     * 인증 메일 재발송 서비스
     * 1. 사용자 조회
     * 2. 이미 활성화된 사용자면 예외 처리
     * 3. 새로운 이메일 인증 토큰 생성
     * 4. 이메일 발송
     */
    public void resendVerificationEmail(String email) {
        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이미 활성화된 사용자면 예외 처리
        if (user.getUserStatus() == UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.ALREADY_VERIFIED);
        }

        // TODO: 기존 토큰이 남아있는 경우 삭제하는 로직 추가 고려

        // 새로운 이메일 인증 토큰 생성
        String emailToken = tokenService.createEmailVerificationToken(user.getId());

        // 이메일 발송
        emailService.sendVerificationEmail(user.getEmail(), emailToken);
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
     * 아이디 찾기 서비스
     * 1. 이메일로 사용자 조회
     * 2. username 일부 마스킹 처리
     * 3. 이메일 발송
     */
    public void recoverUsername(String email) {
        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // username 일부 마스킹 처리
        String maskedUsername = maskUsername(user.getUsername());

        // 이메일 발송
        emailService.sendUsernameEmail(user.getEmail(), maskedUsername);
    }

    /**
     * 비밀번호 재설정 요청 서비스
     * 1. 이메일로 사용자 조회
     * 2. 비밀번호 재설정 토큰 생성
     * 3. 이메일 발송
     */
    public void recoverPassword(String email) {
        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 재설정 토큰 생성
        String resetToken = tokenService.createPasswordResetToken(user.getId());

        // 이메일 발송
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    /**
     * 비밀번호 재설정 서비스
     * 1. 토큰 검증
     * 2. 사용자 조회
     * 3. 비밀번호 정책 검증
     * 4. 비밀번호 변경
     * 5. 토큰 삭제
     */
    public void resetPassword(String token, String newPassword) {
        // 토큰 존재 여부 확인
        if (token == null || token.isEmpty()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // 토큰으로 사용자 ID 조회
        Long userId = tokenService.getUserIdByPasswordResetToken(token);
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 정책 검증
        PasswordValidator.validate(newPassword);

        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(newPassword));

        // 토큰 삭제 (재사용 방지)
        tokenService.deletePasswordResetToken(token);
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

    /**
     * username 일부 마스킹 처리
     * - 1~2글자 → 첫 글자만 보이고 나머지는 * (ex. a*)
     * - 3~4글자 → 앞 1글자 + * + 뒤 1글자 (ex. a*c, a**d)
     * - 5글자 이상 → 앞 2글자 + * + 뒤 2글자 (ex. ab*de, ab**ef)
     */
    private String maskUsername(String username) {
        if (username.length() <= 2) {
            return username.charAt(0) + "*";
        }
        int length = username.length();
        if (length <= 2) {
            // 1~2글자 → 첫 글자만 보이고 나머지는 *
            return username.charAt(0) + "*".repeat(length - 1);
        } else if (length <= 4) {
            // 3~4글자 → 앞 1글자 + * + 뒤 1글자
            return username.charAt(0)
                    + "*".repeat(length - 2)
                    + username.charAt(length - 1);
        } else {
            // 5글자 이상 → 앞 2글자 + * + 뒤 2글자
            return username.substring(0, 2)
                    + "*".repeat(length - 4)
                    + username.substring(length - 2);
        }
    }
}
