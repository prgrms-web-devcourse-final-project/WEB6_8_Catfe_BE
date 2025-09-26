package com.back.domain.user.controller;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.fixture.TestJwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

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
    private TestJwtTokenProvider testJwtTokenProvider;

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
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("홍길동"));

        // DB에서 저장된 User 상태 검증
        User saved = userRepository.findByUsername("testuser").orElseThrow();
        // TODO: 이메일 인증 기능 개발 후 기본 상태를 PENDING으로 변경
//        assertThat(saved.getUserStatus()).isEqualTo(UserStatus.PENDING);
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
                .andExpect(jsonPath("$.code").value("USER_002"));
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
                .andExpect(jsonPath("$.code").value("USER_003"));
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
                .andExpect(jsonPath("$.code").value("USER_004"));
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
                .andExpect(jsonPath("$.code").value("USER_005"));
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
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    @DisplayName("정상 로그인 → 200 OK + accessToken + refreshToken 쿠키")
    void login_success() throws Exception {
        // given: 회원가입 요청으로 DB에 정상 유저 저장
        String rawPassword = "P@ssw0rd!";
        String registerBody = """
                {
                  "username": "loginuser",
                  "email": "login@example.com",
                  "password": "%s",
                  "nickname": "홍길동"
                }
                """.formatted(rawPassword);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

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
                .andExpect(jsonPath("$.data.user.username").value("loginuser"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("잘못된 비밀번호 → 401 Unauthorized")
    void login_invalidPassword() throws Exception {
        // given: 정상 유저를 회원가입으로 저장
        String rawPassword = "P@ssw0rd!";
        String registerBody = """
                {
                  "username": "badpwuser",
                  "email": "badpw@example.com",
                  "password": "%s",
                  "nickname": "닉네임"
                }
                """.formatted(rawPassword);
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody));

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
                .andExpect(jsonPath("$.code").value("USER_006"));
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
                .andExpect(jsonPath("$.code").value("USER_006"));
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

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
                .andExpect(jsonPath("$.code").value("USER_007"));
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
                .andExpect(jsonPath("$.code").value("USER_008"));
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
                .andExpect(jsonPath("$.code").value("USER_009"));
    }

    @Test
    @DisplayName("정상 로그아웃 → 200 OK + RefreshToken 쿠키 만료")
    void logout_success() throws Exception {
        // given: 회원가입 + 로그인으로 refreshToken 쿠키 확보
        String rawPassword = "P@ssw0rd!";
        String registerBody = """
            {
              "username": "logoutuser",
              "email": "logout@example.com",
              "password": "%s",
              "nickname": "홍길동"
            }
            """.formatted(rawPassword);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

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

        // when: 로그아웃 요청 (쿠키 포함)
        ResultActions logoutResult = mvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", refreshCookie)))
                .andDo(print());

        // then: 200 OK + 성공 메시지 + 쿠키 만료
        logoutResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("RefreshToken 누락 → 400 Bad Request")
    void logout_noToken() throws Exception {
        // when & then
        mvc.perform(post("/api/auth/logout"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken → 401 Unauthorized")
    void logout_invalidToken() throws Exception {
        // given: 잘못된 refreshToken 쿠키
        Cookie invalid = new Cookie("refreshToken", "fake-token");

        // when & then
        mvc.perform(post("/api/auth/logout").cookie(invalid))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    @DisplayName("정상 토큰 재발급 → 200 OK + 새로운 AccessToken 반환")
    void refreshToken_success() throws Exception {
        // given: 회원가입 + 로그인 → 기존 토큰 확보
        String rawPassword = "P@ssw0rd!";
        String registerBody = """
            {
              "username": "refreshuser",
              "email": "refresh@example.com",
              "password": "%s",
              "nickname": "홍길동"
            }
            """.formatted(rawPassword);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

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

        // 기존 AccessToken, RefreshToken 확보
        String oldAccessToken = loginResult.andReturn()
                .getResponse()
                .getContentAsString(); // body에서 accessToken 꺼낼 수 있도록 JSON 파싱 필요

        String refreshCookie = loginResult.andReturn()
                .getResponse()
                .getCookie("refreshToken")
                .getValue();

        // when: 재발급 요청 (RefreshToken 쿠키 포함)
        ResultActions refreshResult = mvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshCookie)))
                .andDo(print());

        // then: 200 OK + 새로운 AccessToken 발급
        refreshResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());

        // 새 토큰은 기존 토큰과 달라야 함 (파싱 후 비교)
//        String newAccessToken = JsonPath.read(refreshResult.andReturn()
//                .getResponse().getContentAsString(), "$.data.accessToken");
//        assertThat(newAccessToken).isNotEqualTo(oldAccessToken);
    }

    @Test
    @DisplayName("RefreshToken 누락 → 400 Bad Request")
    void refreshToken_noToken() throws Exception {
        mvc.perform(post("/api/auth/refresh"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken → 401 Unauthorized")
    void refreshToken_invalidToken() throws Exception {
        Cookie invalid = new Cookie("refreshToken", "fake-token");

        mvc.perform(post("/api/auth/refresh").cookie(invalid))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"));
    }

    @Test
    @DisplayName("만료된 RefreshToken → 401 Unauthorized")
    void refreshToken_expiredToken() throws Exception {
        // given: 이미 만료된 Refresh Token 발급
        User user = User.createUser("expiredUser", "expired@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        userRepository.save(user);

        // JwtTokenProvider에 테스트용 메서드 추가 필요
        String expiredRefreshToken = testJwtTokenProvider.createExpiredRefreshToken(user.getId());

        Cookie expiredCookie = new Cookie("refreshToken", expiredRefreshToken);

        // when & then
        mvc.perform(post("/api/auth/refresh").cookie(expiredCookie))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401"))
                .andExpect(jsonPath("$.message").value("만료된 리프레시 토큰입니다."));
    }
}