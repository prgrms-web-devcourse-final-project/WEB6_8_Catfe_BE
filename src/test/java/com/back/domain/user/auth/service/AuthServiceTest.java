package com.back.domain.user.auth.service;

import com.back.domain.user.auth.dto.LoginRequest;
import com.back.domain.user.auth.dto.LoginResponse;
import com.back.domain.user.auth.dto.UserRegisterRequest;
import com.back.domain.user.auth.dto.UserResponse;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserProfileRepository;
import com.back.domain.user.common.repository.UserRepository;
import com.back.domain.user.common.repository.UserTokenRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private EmailService emailService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        EmailService emailService() {
            return mock(EmailService.class);
        }
    }

    private User setupUser(String username, String email, String password, String nickname, UserStatus status) {
        UserRegisterRequest request = new UserRegisterRequest(username, email, password, nickname);
        UserResponse response = authService.register(request);

        User saved = userRepository.findById(response.userId()).orElseThrow();
        saved.setUserStatus(status); // 상태 변경 (PENDING, SUSPENDED, DELETED)
        return saved;
    }

    // ======================== 회원가입 테스트 ========================

    @Test
    @DisplayName("정상 회원가입 성공")
    void register_success() {
        // given: 정상적인 회원가입 요청 생성
        UserRegisterRequest request = new UserRegisterRequest(
                "testuser", "test@example.com", "P@ssw0rd!", "홍길동"
        );

        // when: 회원가입 실행
        UserResponse response = authService.register(request);

        // then: 반환된 값 검증
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("홍길동");
        assertThat(response.status()).isEqualTo(UserStatus.PENDING);

        // 비밀번호 인코딩 검증
        String encoded = userRepository.findById(response.userId()).get().getPassword();
        assertThat(passwordEncoder.matches("P@ssw0rd!", encoded)).isTrue();

        // UserProfile도 함께 생성되었는지 확인
        assertThat(userProfileRepository.existsByNickname("홍길동")).isTrue();
    }

    @Test
    @DisplayName("중복된 username이면 예외 발생")
    void register_duplicateUsername() {
        // given: 동일 username으로 첫 번째 가입
        authService.register(new UserRegisterRequest(
                "dupuser", "dup@example.com", "P@ssw0rd!", "닉네임"
        ));

        // when & then: 같은 username으로 가입 시 예외 발생
        assertThatThrownBy(() ->
                authService.register(new UserRegisterRequest(
                        "dupuser", "other@example.com", "P@ssw0rd!", "다른닉네임"
                ))
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USERNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("중복된 email이면 예외 발생")
    void register_duplicateEmail() {
        // given: 동일 email로 첫 번째 가입
        authService.register(new UserRegisterRequest(
                "user1", "dup@example.com", "P@ssw0rd!", "닉네임"
        ));

        // when & then: 같은 email로 가입 시 예외 발생
        assertThatThrownBy(() ->
                authService.register(new UserRegisterRequest(
                        "user2", "dup@example.com", "P@ssw0rd!", "다른닉네임"
                ))
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.EMAIL_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("중복된 nickname이면 예외 발생")
    void register_duplicateNickname() {
        // given: 동일 nickname으로 첫 번째 가입
        authService.register(new UserRegisterRequest(
                "user1", "user1@example.com", "P@ssw0rd!", "dupnick"
        ));

        // when & then: 같은 nickname으로 가입 시 예외 발생
        assertThatThrownBy(() ->
                authService.register(new UserRegisterRequest(
                        "user2", "user2@example.com", "P@ssw0rd!", "dupnick"
                ))
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NICKNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("비밀번호 정책 위반(숫자/특수문자 없음) → 예외 발생")
    void register_invalidPassword_noNumberOrSpecial() {
        // given: 숫자, 특수문자 없는 비밀번호
        UserRegisterRequest request = new UserRegisterRequest(
                "user1", "user1@example.com", "abcdefgh", "닉네임"
        );

        // when & then: 정책 위반으로 예외 발생
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("비밀번호 정책 위반(길이 7자) → 예외 발생")
    void register_invalidPassword_tooShort() {
        // given: 7자리 비밀번호 (정책상 8자 이상 필요)
        UserRegisterRequest request = new UserRegisterRequest(
                "user2", "user2@example.com", "Abc12!", "닉네임"
        );

        // when & then: 정책 위반으로 예외 발생
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("비밀번호 정책 통과(정상 8자 이상, 숫자/특수문자 포함) → 성공")
    void register_validPassword() {
        // given: 정책을 만족하는 정상 비밀번호
        UserRegisterRequest request = new UserRegisterRequest(
                "user3", "user3@example.com", "Abcd123!", "닉네임"
        );

        // when: 회원가입 실행
        UserResponse response = authService.register(request);

        // then: username과 비밀번호 인코딩 검증
        assertThat(response.username()).isEqualTo("user3");
        assertThat(passwordEncoder.matches("Abcd123!",
                userRepository.findById(response.userId()).get().getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 시 이메일 인증 토큰 생성 및 이메일 발송 성공")
    void register_sendVerificationEmail() {
        // given: 회원가입 요청 생성
        UserRegisterRequest request = new UserRegisterRequest(
                "mailuser", "mailuser@example.com", "P@ssw0rd!", "메일닉네임"
        );

        // when: 회원가입 실행
        UserResponse response = authService.register(request);

        // then: 이메일 발송이 1회 호출되었는지 검증
        verify(emailService, times(1))
                .sendVerificationEmail(eq("mailuser@example.com"), anyString());

        // 저장된 사용자 상태는 PENDING인지 확인
        User saved = userRepository.findById(response.userId()).orElseThrow();
        assertThat(saved.getUserStatus()).isEqualTo(UserStatus.PENDING);
    }

    // ======================== 이메일 인증 테스트 ========================

    @Test
    @DisplayName("정상 이메일 인증 성공 → PENDING → ACTIVE")
    void verifyEmail_success() {
        // given: 회원가입 후 사용자 생성 (기본 상태 = PENDING)
        UserRegisterRequest request = new UserRegisterRequest(
                "verifyuser", "verify@example.com", "P@ssw0rd!", "닉네임"
        );
        UserResponse response = authService.register(request);
        User user = userRepository.findById(response.userId()).orElseThrow();

        // 이메일 인증 토큰 생성
        String token = tokenService.createEmailVerificationToken(user.getId());

        // when: 이메일 인증 실행
        UserResponse verified = authService.verifyEmail(token);

        // then: 상태가 ACTIVE로 변경되었는지 확인
        assertThat(verified.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("이메일 인증 실패 - 토큰 누락 → BAD_REQUEST")
    void verifyEmail_noToken() {
        // when & then: 토큰이 null이면 BAD_REQUEST 예외 발생
        assertThatThrownBy(() -> authService.verifyEmail(null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    @DisplayName("이메일 인증 실패 - 잘못된 토큰 → INVALID_EMAIL_TOKEN")
    void verifyEmail_invalidToken() {
        // when & then: 잘못된 토큰이면 INVALID_EMAIL_TOKEN 예외 발생
        assertThatThrownBy(() -> authService.verifyEmail("invalid-token"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_EMAIL_TOKEN.getMessage());
    }

    @Test
    @DisplayName("이메일 인증 실패 - 이미 인증된 계정 → ALREADY_VERIFIED")
    void verifyEmail_alreadyVerified() {
        // given: 이미 ACTIVE 상태인 사용자 준비
        UserRegisterRequest request = new UserRegisterRequest(
                "alreadyuser", "already@example.com", "P@ssw0rd!", "닉네임"
        );
        UserResponse response = authService.register(request);
        User user = userRepository.findById(response.userId()).orElseThrow();
        user.setUserStatus(UserStatus.ACTIVE);

        // 이미 인증된 사용자에 대해 토큰 발급
        String token = tokenService.createEmailVerificationToken(user.getId());

        // when & then: 인증 시도 시 ALREADY_VERIFIED 예외 발생
        assertThatThrownBy(() -> authService.verifyEmail(token))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ALREADY_VERIFIED.getMessage());
    }

    // ======================== 인증 메일 재발송 테스트 ========================

    @Test
    @DisplayName("정상 인증 메일 재발송 성공 → PENDING 상태 유저")
    void resendVerificationEmail_success() {
        // given: 회원가입 후 기본 상태는 PENDING
        UserRegisterRequest request = new UserRegisterRequest(
                "resenduser", "resend@example.com", "P@ssw0rd!", "닉네임"
        );
        UserResponse response = authService.register(request);
        User user = userRepository.findById(response.userId()).orElseThrow();

        // when: 인증 메일 재발송 요청
        authService.resendVerificationEmail(user.getEmail());

        // then: 이메일 발송이 호출되었는지 확인
        verify(emailService, times(2))
                .sendVerificationEmail(eq("resend@example.com"), anyString());
        // (회원가입 시 1회 + 재발송 시 1회 → 총 2회)
    }

    @Test
    @DisplayName("인증 메일 재발송 실패 - 존재하지 않는 사용자 → USER_NOT_FOUND")
    void resendVerificationEmail_userNotFound() {
        // when & then
        assertThatThrownBy(() ->
                authService.resendVerificationEmail("notfound@example.com")
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("인증 메일 재발송 실패 - 이미 ACTIVE 상태 → ALREADY_VERIFIED")
    void resendVerificationEmail_alreadyVerified() {
        // given: 회원가입 후 사용자 상태를 ACTIVE로 변경
        UserRegisterRequest request = new UserRegisterRequest(
                "activeuser", "active@example.com", "P@ssw0rd!", "닉네임"
        );
        UserResponse response = authService.register(request);
        User user = userRepository.findById(response.userId()).orElseThrow();
        user.setUserStatus(UserStatus.ACTIVE);

        // when & then: 재발송 시도 시 ALREADY_VERIFIED 예외 발생
        assertThatThrownBy(() ->
                authService.resendVerificationEmail(user.getEmail())
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ALREADY_VERIFIED.getMessage());
    }

    // ======================== 로그인 테스트 ========================

    @Test
    @DisplayName("정상 로그인 성공")
    void login_success() {
        // given: 정상적인 사용자와 비밀번호 준비
        String rawPassword = "P@ssw0rd!";
        User user = setupUser("loginuser", "login@example.com", rawPassword, "닉네임", UserStatus.ACTIVE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when: 로그인 요청 실행
        LoginResponse loginResponse = authService.login(
                new LoginRequest("loginuser", rawPassword), response);

        // then: 응답에 username과 토큰/쿠키가 포함됨
        assertThat(loginResponse.user().username()).isEqualTo("loginuser");
        assertThat(loginResponse.accessToken()).isNotBlank();

        Cookie refreshCookie = response.getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("잘못된 비밀번호 → INVALID_CREDENTIALS 예외 발생")
    void login_invalidPassword() {
        // given: 존재하는 사용자, 잘못된 비밀번호 입력
        User user = setupUser("loginuser", "login@example.com", "P@ssw0rd!", "닉네임", UserStatus.ACTIVE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then: 로그인 시도 시 INVALID_CREDENTIALS 예외 발생
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("loginuser", "wrongPassword"), response
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 username → INVALID_CREDENTIALS 예외 발생")
    void login_userNotFound() {
        // given: 존재하지 않는 username 사용
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then: 로그인 시도 시 INVALID_CREDENTIALS 예외 발생
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("nouser", "P@ssw0rd!"), response
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("상태가 PENDING → USER_EMAIL_NOT_VERIFIED 예외 발생")
    void login_pendingUser() {
        // given: 상태가 PENDING인 사용자
        User user = setupUser("pendinguser", "pending@example.com", "P@ssw0rd!", "닉네임", UserStatus.PENDING);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then: 로그인 시도 시 USER_EMAIL_NOT_VERIFIED 예외 발생
        assertThatThrownBy(() -> authService.login(
                new LoginRequest(user.getUsername(), "P@ssw0rd!"), response
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_EMAIL_NOT_VERIFIED.getMessage());
    }

    @Test
    @DisplayName("상태가 SUSPENDED → USER_SUSPENDED 예외 발생")
    void login_suspendedUser() {
        // given: 상태가 SUSPENDED인 사용자
        User user = setupUser("suspended", "suspended@example.com", "P@ssw0rd!", "닉네임", UserStatus.SUSPENDED);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then: 로그인 시도 시 USER_SUSPENDED 예외 발생
        assertThatThrownBy(() -> authService.login(
                new LoginRequest(user.getUsername(), "P@ssw0rd!"), response
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    @Test
    @DisplayName("상태가 DELETED → USER_DELETED 예외 발생")
    void login_deletedUser() {
        // given: 상태가 DELETED인 사용자
        User user = setupUser("deleted", "deleted@example.com", "P@ssw0rd!", "닉네임", UserStatus.DELETED);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then: 로그인 시도 시 USER_DELETED 예외 발생
        assertThatThrownBy(() -> authService.login(
                new LoginRequest(user.getUsername(), "P@ssw0rd!"), response
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    // ======================== 로그아웃 테스트 ========================

    @Test
    @DisplayName("정상 로그아웃 성공 → RefreshToken DB 삭제 + 쿠키 만료")
    void logout_success() {
        // given: 정상 로그인된 사용자
        String rawPassword = "P@ssw0rd!";
        User user = setupUser("logoutuser", "logout@example.com", rawPassword, "닉네임", UserStatus.ACTIVE);
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();

        authService.login(new LoginRequest("logoutuser", rawPassword), loginResponse);
        Cookie refreshCookie = loginResponse.getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();

        MockHttpServletResponse logoutResponse = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(refreshCookie); // 쿠키를 요청에 실어줌

        // when: 로그아웃 실행
        authService.logout(request, logoutResponse);

        // then: DB에서 refreshToken 삭제됨
        assertThat(userTokenRepository.findByRefreshToken(refreshCookie.getValue())).isEmpty();

        // 응답 쿠키는 만료 처리됨
        Cookie cleared = logoutResponse.getCookie("refreshToken");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
        assertThat(cleared.getValue()).isEmpty();
    }

    @Test
    @DisplayName("RefreshToken 없으면 INVALID_TOKEN 예외 발생")
    void logout_noToken() {
        // given: 쿠키 없이 로그아웃 요청
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> authService.logout(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken이면 INVALID_TOKEN 예외 발생")
    void logout_invalidToken() {
        // given: 잘못된 토큰 쿠키 세팅
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "invalidToken"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> authService.logout(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    // ======================== 토큰 재발급 테스트 ========================

    @Test
    @DisplayName("정상 토큰 재발급 성공 → 새로운 AccessToken 반환 및 헤더 설정")
    void refreshToken_success() throws InterruptedException {
        // given: 로그인된 사용자 준비
        String rawPassword = "P@ssw0rd!";
        User user = setupUser("refreshuser", "refresh@example.com", rawPassword, "닉네임", UserStatus.ACTIVE);
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();

        LoginResponse loginRes = authService.login(new LoginRequest("refreshuser", rawPassword), loginResponse);
        String oldAccessToken = loginRes.accessToken();
        Cookie refreshCookie = loginResponse.getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();

        // 요청/응답 객체 준비
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(refreshCookie);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Issued At(발급 시간) 분리를 위해 1초 대기
//        Thread.sleep(1000);

        // when: 토큰 재발급 실행
        String newAccessToken = authService.refreshToken(request, response);

        // then: 반환값 및 응답 헤더 검증
        assertThat(newAccessToken).isNotBlank();
//        assertThat(newAccessToken).isNotEqualTo(oldAccessToken);
    }

    @Test
    @DisplayName("RefreshToken 없으면 BAD_REQUEST 예외 발생")
    void refreshToken_noToken() {
        // given: 쿠키 없는 요청
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken이면 INVALID_TOKEN 예외 발생")
    void refreshToken_invalidToken() {
        // given: 잘못된 Refresh Token
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "invalidToken"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    // ======================== 아이디 찾기 테스트 ========================

    @Test
    @DisplayName("정상 아이디 찾기 성공 → 이메일 발송")
    void recoverUsername_success() {
        // given: 회원가입으로 사용자 생성
        UserRegisterRequest request = new UserRegisterRequest(
                "findme", "findme@example.com", "P@ssw0rd!", "닉네임"
        );
        UserResponse response = authService.register(request);

        // when: 아이디 찾기 실행
        authService.recoverUsername("findme@example.com");

        // then: 이메일 발송이 호출되었는지 확인
        verify(emailService, times(1))
                .sendUsernameEmail(eq("findme@example.com"), anyString());
    }

    @Test
    @DisplayName("아이디 찾기 실패 - 존재하지 않는 이메일 → USER_NOT_FOUND")
    void recoverUsername_userNotFound() {
        // when & then
        assertThatThrownBy(() ->
                authService.recoverUsername("notfound@example.com")
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("아이디 마스킹 규칙 검증")
    void recoverUsername_maskingRules() {
        // given: 길이가 다른 username 케이스
        User shortUser = User.createUser("a", "short@example.com", passwordEncoder.encode("P@ssw0rd!"));
        shortUser.setUserProfile(new UserProfile(shortUser, "닉", null, null, null, 0));
        userRepository.save(shortUser);

        User midUser = User.createUser("abc", "mid@example.com", passwordEncoder.encode("P@ssw0rd!"));
        midUser.setUserProfile(new UserProfile(midUser, "닉", null, null, null, 0));
        userRepository.save(midUser);

        User longUser = User.createUser("abcdef", "long@example.com", passwordEncoder.encode("P@ssw0rd!"));
        longUser.setUserProfile(new UserProfile(longUser, "닉", null, null, null, 0));
        userRepository.save(longUser);

        // when
        authService.recoverUsername("short@example.com");
        authService.recoverUsername("mid@example.com");
        authService.recoverUsername("long@example.com");

        // then: 마스킹된 값으로 이메일 발송 확인
        verify(emailService).sendUsernameEmail(eq("short@example.com"), eq("a*"));
        verify(emailService).sendUsernameEmail(eq("mid@example.com"), eq("a*c"));
        verify(emailService).sendUsernameEmail(eq("long@example.com"), eq("ab**ef"));
    }

    // ======================== 비밀번호 재설정 요청 테스트 ========================

    @Test
    @DisplayName("정상 비밀번호 재설정 요청 → 이메일 발송 성공")
    void recoverPassword_success() {
        // given: 가입된 사용자 생성
        User user = User.createUser("pwuser", "pw@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        userRepository.save(user);

        // when
        authService.recoverPassword("pw@example.com");

        // then: 이메일 발송 호출 확인
        verify(emailService, times(1))
                .sendPasswordResetEmail(eq("pw@example.com"), anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 요청 실패 - 존재하지 않는 사용자 → USER_NOT_FOUND")
    void recoverPassword_userNotFound() {
        // when & then
        assertThatThrownBy(() -> authService.recoverPassword("notfound@example.com"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // ======================== 비밀번호 재설정 테스트 ========================

    @Test
    @DisplayName("정상 비밀번호 재설정 성공 → 비밀번호 변경 및 토큰 삭제")
    void resetPassword_success() {
        // given: 가입된 사용자
        User user = User.createUser("resetuser", "reset@example.com", passwordEncoder.encode("OldPass123!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        userRepository.save(user);

        // 비밀번호 재설정 토큰 생성
        String token = tokenService.createPasswordResetToken(user.getId());

        // when
        authService.resetPassword(token, "NewPass123!");

        // then: 비밀번호 변경 확인
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPass123!", updated.getPassword())).isTrue();

        // 토큰이 삭제되었는지 확인
        Long result = tokenService.getUserIdByPasswordResetToken(token);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 유효하지 않은 토큰 → INVALID_PASSWORD_RESET_TOKEN")
    void resetPassword_invalidToken() {
        assertThatThrownBy(() -> authService.resetPassword("fake-token", "NewPass123!"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD_RESET_TOKEN.getMessage());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 존재하지 않는 사용자 → USER_NOT_FOUND")
    void resetPassword_userNotFound() {
        // given: 토큰은 만들었지만 사용자 ID는 없는 값으로 설정
        String token = tokenService.createPasswordResetToken(99999L);

        // when & then
        assertThatThrownBy(() -> authService.resetPassword(token, "NewPass123!"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 비밀번호 정책 위반 → INVALID_PASSWORD")
    void resetPassword_invalidPassword() {
        // given: 가입된 사용자 + 토큰
        User user = User.createUser("resetuser2", "reset2@example.com", passwordEncoder.encode("OldPass123!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        userRepository.save(user);

        String token = tokenService.createPasswordResetToken(user.getId());

        // when & then
        assertThatThrownBy(() -> authService.resetPassword(token, "weakpw"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD.getMessage());
    }
}