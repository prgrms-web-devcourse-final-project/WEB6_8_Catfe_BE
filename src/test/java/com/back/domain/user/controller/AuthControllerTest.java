package com.back.domain.user.controller;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.domain.user.service.EmailService;
import com.back.domain.user.service.TokenService;
import com.back.fixture.TestJwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public EmailService emailService() {
            return mock(EmailService.class);
        }
    }

    // ======================== 회원가입 테스트 ========================

    @Test
    @DisplayName("정상 회원가입 → 201 Created")
    void register_success() throws Exception {
        // given: 정상적인 회원가입 요청 JSON
        String body = """
                {
                  "username": "testuser",
                  "email": "test@example.com",
                  "password": "P@ssw0rd!",
                  "nickname": "홍길동"
                }
                """;

        // when: 회원가입 API 호출
        ResultActions resultActions = mvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andDo(print());

        // then: 응답 값과 DB 저장값 검증
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다. 이메일 인증을 완료해주세요."))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("홍길동"));

        // DB에서 저장된 User 상태 검증
        User saved = userRepository.findByUsername("testuser").orElseThrow();
        assertThat(saved.getUserStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    @DisplayName("중복 username → 409 Conflict")
    void register_duplicateUsername() throws Exception {
        // given: 이미 존재하는 username을 가진 User 저장
        User existing = User.createUser("dupuser", "dup@example.com", "password123!");
        existing.setUserProfile(new UserProfile(existing, "dupnick", null, null, null, 0));
        userRepository.save(existing);

        // 동일 username으로 회원가입 요청
        String body = """
                {
                  "username": "dupuser",
                  "email": "other@example.com",
                  "password": "P@ssw0rd!",
                  "nickname": "다른닉네임"
                }
                """;

        // when & then: 409 Conflict 응답 및 에러 코드 확인
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_002"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."));
    }

    @Test
    @DisplayName("중복 email → 409 Conflict")
    void register_duplicateEmail() throws Exception {
        // given: 이미 존재하는 email을 가진 User 저장
        User existing = User.createUser("user1", "dup@example.com", "password123!");
        existing.setUserProfile(new UserProfile(existing, "nick1", null, null, null, 0));
        userRepository.save(existing);

        // 동일 email로 회원가입 요청
        String body = """
                {
                  "username": "otheruser",
                  "email": "dup@example.com",
                  "password": "P@ssw0rd!",
                  "nickname": "다른닉네임"
                }
                """;

        // when & then: 409 Conflict 응답 및 에러 코드 확인
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_003"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("중복 nickname → 409 Conflict")
    void register_duplicateNickname() throws Exception {
        // given: 이미 존재하는 nickname을 가진 User 저장
        User existing = User.createUser("user2", "user2@example.com", "password123!");
        existing.setUserProfile(new UserProfile(existing, "dupnick", null, null, null, 0));
        userRepository.save(existing);

        // 동일 nickname으로 회원가입 요청
        String body = """
                {
                  "username": "newuser",
                  "email": "new@example.com",
                  "password": "P@ssw0rd!",
                  "nickname": "dupnick"
                }
                """;

        // when & then: 409 Conflict 응답 및 에러 코드 확인
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_004"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("비밀번호 정책 위반 → 400 Bad Request")
    void register_invalidPassword() throws Exception {
        // given: 숫자/특수문자 포함 안 된 약한 비밀번호
        String body = """
                {
                  "username": "weakpw",
                  "email": "weak@example.com",
                  "password": "password",
                  "nickname": "닉네임"
                }
                """;

        // when & then: 400 Bad Request 응답 및 에러 코드 확인
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("비밀번호는 최소 8자 이상, 숫자/특수문자를 포함해야 합니다."));
    }

    @Test
    @DisplayName("잘못된 요청값 (필수 필드 누락) → 400 Bad Request")
    void register_invalidRequest_missingField() throws Exception {
        // given: 필수 값 누락 (username, password, nickname 비어있음 / email 형식 잘못됨)
        String body = """
                {
                  "username": "",
                  "email": "invalid",
                  "password": "",
                  "nickname": ""
                }
                """;

        // when & then: 400 Bad Request 응답 및 공통 에러 코드 확인
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    // ======================== 이메일 인증 테스트 ========================

    @Test
    @DisplayName("이메일 인증 성공 → 200 OK + UserStatus.ACTIVE")
    void verifyEmail_success() throws Exception {
        // given: PENDING 상태 유저 생성
        User pending = User.createUser("verifyuser", "verify@example.com",
                passwordEncoder.encode("P@ssw0rd!"));
        pending.setUserProfile(new UserProfile(pending, "홍길동", null, null, null, 0));
        pending.setUserStatus(UserStatus.PENDING);
        User saved = userRepository.save(pending);

        // 이메일 인증 토큰 발급
        String token = tokenService.createEmailVerificationToken(saved.getId());

        // when & then
        mvc.perform(get("/api/auth/email-verification").param("token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."))
                .andExpect(jsonPath("$.data.username").value("verifyuser"))
                .andExpect(jsonPath("$.data.email").value("verify@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("홍길동"));
    }

    @Test
    @DisplayName("유효하지 않거나 만료된 토큰 → 401 Unauthorized")
    void verifyEmail_invalidOrExpiredToken() throws Exception {
        mvc.perform(get("/api/auth/email-verification").param("token", "fake-token"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TOKEN_001"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 이메일 인증 토큰입니다."));
    }

    @Test
    @DisplayName("이미 인증된 계정 → 409 Conflict")
    void verifyEmail_alreadyVerified() throws Exception {
        // given: ACTIVE 상태 유저
        User active = User.createUser("activeuser", "active@example.com",
                passwordEncoder.encode("P@ssw0rd!"));
        active.setUserProfile(new UserProfile(active, "홍길동", null, null, null, 0));
        active.setUserStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(active);

        // 토큰 발급 (실제로는 필요 없지만, 호출 상황 가정)
        String token = tokenService.createEmailVerificationToken(saved.getId());

        // when & then
        mvc.perform(get("/api/auth/email-verification").param("token", token))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TOKEN_002"))
                .andExpect(jsonPath("$.message").value("이미 인증된 계정입니다."));
    }

    @Test
    @DisplayName("토큰 파라미터 누락 → 400 Bad Request")
    void verifyEmail_missingToken() throws Exception {
        mvc.perform(get("/api/auth/email-verification"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    // ======================== 인증 메일 재발송 테스트 ========================

    @Test
    @DisplayName("인증 메일 재발송 성공 → 200 OK")
    void resendVerificationEmail_success() throws Exception {
        // given: PENDING 상태 유저 생성
        User pending = User.createUser("resenduser", "resend@example.com",
                passwordEncoder.encode("P@ssw0rd!"));
        pending.setUserProfile(new UserProfile(pending, "재발송닉", null, null, null, 0));
        pending.setUserStatus(UserStatus.PENDING);
        userRepository.save(pending);

        String body = """
                {
                  "email": "resend@example.com"
                }
                """;

        // when & then
        mvc.perform(post("/api/auth/email-verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("인증 메일이 재발송되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("인증 메일 재발송 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void resendVerificationEmail_userNotFound() throws Exception {
        String body = """
                {
                  "email": "notfound@example.com"
                }
                """;

        mvc.perform(post("/api/auth/email-verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("인증 메일 재발송 실패 - 이미 인증된 계정 → 409 Conflict")
    void resendVerificationEmail_alreadyVerified() throws Exception {
        // given: ACTIVE 상태 유저 생성
        User active = User.createUser("activeuser2", "active2@example.com",
                passwordEncoder.encode("P@ssw0rd!"));
        active.setUserProfile(new UserProfile(active, "액티브닉", null, null, null, 0));
        active.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(active);

        String body = """
                {
                  "email": "active2@example.com"
                }
                """;

        mvc.perform(post("/api/auth/email-verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TOKEN_002"))
                .andExpect(jsonPath("$.message").value("이미 인증된 계정입니다."));
    }

    @Test
    @DisplayName("인증 메일 재발송 실패 - 이메일 필드 누락 → 400 Bad Request")
    void resendVerificationEmail_missingField() throws Exception {
        // given: 잘못된 요청 (이메일 누락)
        String body = """
                {
                }
                """;

        mvc.perform(post("/api/auth/email-verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    // ======================== 로그인 테스트 ========================

    @Test
    @DisplayName("정상 로그인 → 200 OK + accessToken + refreshToken 쿠키")
    void login_success() throws Exception {
        // given: 활성화된 유저 저장 (비밀번호 인코딩 필수)
        String rawPassword = "P@ssw0rd!";
        User user = User.createUser("loginuser", "login@example.com",
                passwordEncoder.encode(rawPassword));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when: 로그인 요청
        String loginBody = """
                {
                  "username": "loginuser",
                  "password": "%s"
                }
                """.formatted(rawPassword);

        ResultActions resultActions = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andDo(print());

        // then: 200 OK 응답 + username/accessToken/refreshToken 쿠키 확인
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.user.username").value("loginuser"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("잘못된 비밀번호 → 401 Unauthorized")
    void login_invalidPassword() throws Exception {
        // given: 정상 유저 저장
        String rawPassword = "P@ssw0rd!";
        User user = User.createUser("loginuser", "login@example.com",
                passwordEncoder.encode(rawPassword));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when: 틀린 비밀번호로 로그인 요청
        String loginBody = """
                {
                  "username": "badpwuser",
                  "password": "WrongPass!"
                }
                """;

        // then: 401 Unauthorized 응답 + 에러 코드 USER_006 확인
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 username → 401 Unauthorized")
    void login_userNotFound() throws Exception {
        // given: 존재하지 않는 username 사용
        String loginBody = """
                {
                  "username": "nouser",
                  "password": "P@ssw0rd!"
                }
                """;

        // when & then: 401 Unauthorized 응답 + 에러 코드 USER_006 확인
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("이메일 미인증(PENDING) 계정 로그인 → 403 Forbidden")
    void login_pendingUser() throws Exception {
        // given: 상태가 PENDING인 유저 저장 (비밀번호 인코딩 필수)
        User pending = User.createUser("pending", "pending@example.com",
                passwordEncoder.encode("P@ssw0rd!"));
        pending.setUserProfile(new UserProfile(pending, "닉네임", null, null, null, 0));
        pending.setUserStatus(UserStatus.PENDING);
        userRepository.save(pending);

        String body = """
                {
                  "username": "pending",
                  "password": "P@ssw0rd!"
                }
                """;

        // when & then: 403 Forbidden 응답 + 에러 코드 USER_007 확인
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_007"))
                .andExpect(jsonPath("$.message").value("이메일 인증 후 로그인할 수 있습니다."));
    }

    @Test
    @DisplayName("정지된 계정(SUSPENDED) 로그인 → 403 Forbidden")
    void login_suspendedUser() throws Exception {
        // given: 상태가 SUSPENDED인 유저 저장
        User suspended = User.createUser("suspended", "suspended@example.com",
                passwordEncoder.encode("P@ssw0rd!"));
        suspended.setUserProfile(new UserProfile(suspended, "닉네임", null, null, null, 0));
        suspended.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(suspended);

        String body = """
                {
                  "username": "suspended",
                  "password": "P@ssw0rd!"
                }
                """;

        // when & then: 403 Forbidden 응답 + 에러 코드 USER_008 확인
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_008"))
                .andExpect(jsonPath("$.message").value("정지된 계정입니다. 관리자에게 문의하세요."));
    }

    @Test
    @DisplayName("탈퇴한 계정(DELETED) 로그인 → 410 Gone")
    void login_deletedUser() throws Exception {
        // given: 상태가 DELETED인 유저 저장
        User deleted = User.createUser("deleted", "deleted@example.com",
                passwordEncoder.encode("P@ssw0rd!"));
        deleted.setUserProfile(new UserProfile(deleted, "닉네임", null, null, null, 0));
        deleted.setUserStatus(UserStatus.DELETED);
        userRepository.save(deleted);

        String body = """
                {
                  "username": "deleted",
                  "password": "P@ssw0rd!"
                }
                """;

        // when & then: 410 Gone 응답 + 에러 코드 USER_009 확인
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("USER_009"))
                .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다."));
    }

    // ======================== 로그아웃 테스트 ========================

    @Test
    @DisplayName("정상 로그아웃 → 200 OK + RefreshToken 쿠키 만료")
    void logout_success() throws Exception {
        // given: 활성화된 유저 생성 후 로그인
        String rawPassword = "P@ssw0rd!";
        User user = User.createUser("logoutuser", "logout@example.com",
                passwordEncoder.encode(rawPassword));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String loginBody = """
                {
                  "username": "logoutuser",
                  "password": "%s"
                }
                """.formatted(rawPassword);

        ResultActions loginResult = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk());

        // 로그인 응답에서 refreshToken 쿠키 추출
        String refreshCookie = loginResult.andReturn()
                .getResponse()
                .getCookie("refreshToken")
                .getValue();

        // when: 로그아웃 요청
        ResultActions logoutResult = mvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", refreshCookie)))
                .andDo(print());

        // then
        logoutResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("RefreshToken 누락 → 400 Bad Request")
    void logout_noToken() throws Exception {
        mvc.perform(post("/api/auth/logout"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("만료된 RefreshToken → 401 Unauthorized")
    void logout_expiredToken() throws Exception {
        // given: 만료된 refreshToken
        String expired = testJwtTokenProvider.createExpiredRefreshToken(1L);
        Cookie expiredCookie = new Cookie("refreshToken", expired);

        // when & then
        mvc.perform(post("/api/auth/logout").cookie(expiredCookie))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_005"))
                .andExpect(jsonPath("$.message").value("만료된 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken → 401 Unauthorized")
    void logout_invalidToken() throws Exception {
        // given: 위조된 refreshToken
        Cookie invalid = new Cookie("refreshToken", "fake-token");

        // when & then
        mvc.perform(post("/api/auth/logout").cookie(invalid))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_003"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 리프레시 토큰입니다."));
    }

    // ======================== 토큰 재발급 테스트 ========================

    @Test
    @DisplayName("정상 토큰 재발급 → 200 OK + 새로운 AccessToken 반환")
    void refreshToken_success() throws Exception {
        // given: 활성화된 유저 생성 후 로그인
        String rawPassword = "P@ssw0rd!";
        User user = User.createUser("refreshuser", "refresh@example.com",
                passwordEncoder.encode(rawPassword));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String loginBody = """
                {
                  "username": "refreshuser",
                  "password": "%s"
                }
                """.formatted(rawPassword);

        ResultActions loginResult = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk());

        // 기존 accessToken 추출
        String oldAccessToken = com.jayway.jsonpath.JsonPath.read(
                loginResult.andReturn().getResponse().getContentAsString(),
                "$.data.accessToken"
        );
        String refreshCookie = loginResult.andReturn()
                .getResponse()
                .getCookie("refreshToken")
                .getValue();

        // Issued At(발급 시간) 분리를 위해 1초 대기
//        Thread.sleep(1000);

        // when: 재발급 요청
        ResultActions refreshResult = mvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshCookie)))
                .andDo(print());

        // then: 200 OK + 새로운 accessToken
        refreshResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").exists());

        // 새로운 토큰이 기존과 달라야 함
//        String newAccessToken = com.jayway.jsonpath.JsonPath.read(
//                refreshResult.andReturn().getResponse().getContentAsString(),
//                "$.data.accessToken"
//        );
//        assertThat(newAccessToken).isNotEqualTo(oldAccessToken);
    }

    @Test
    @DisplayName("RefreshToken 누락 → 400 Bad Request")
    void refreshToken_noToken() throws Exception {
        mvc.perform(post("/api/auth/refresh"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken → 401 Unauthorized")
    void refreshToken_invalidToken() throws Exception {
        Cookie invalid = new Cookie("refreshToken", "fake-token");

        mvc.perform(post("/api/auth/refresh").cookie(invalid))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_003"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 RefreshToken → 401 Unauthorized")
    void refreshToken_expiredToken() throws Exception {
        // given: 이미 만료된 RefreshToken 생성
        User user = User.createUser("expiredUser", "expired@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        userRepository.save(user);

        String expiredRefreshToken = testJwtTokenProvider.createExpiredRefreshToken(user.getId());
        Cookie expiredCookie = new Cookie("refreshToken", expiredRefreshToken);

        // when & then
        mvc.perform(post("/api/auth/refresh").cookie(expiredCookie))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_005"))
                .andExpect(jsonPath("$.message").value("만료된 리프레시 토큰입니다."));
    }
}