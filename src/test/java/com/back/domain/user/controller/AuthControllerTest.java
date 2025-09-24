package com.back.domain.user.controller;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
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
}