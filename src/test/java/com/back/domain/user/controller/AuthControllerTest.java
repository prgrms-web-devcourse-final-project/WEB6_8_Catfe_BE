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
        // given
        String body = """
            {
              "username": "testuser",
              "email": "test@example.com",
              "password": "P@ssw0rd!",
              "nickname": "홍길동"
            }
            """;

        // when
        ResultActions resultActions = mvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andDo(print());

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("홍길동"));

        User saved = userRepository.findByUsername("testuser").orElseThrow();
        assertThat(saved.getUserStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    @DisplayName("중복 username → 409 Conflict")
    void register_duplicateUsername() throws Exception {
        User existing = User.createUser("dupuser", "dup@example.com", "password123!");
        existing.setUserProfile(new UserProfile(existing, "dupnick", null, null, null, 0));
        userRepository.save(existing);

        String body = """
            {
              "username": "dupuser",
              "email": "other@example.com",
              "password": "P@ssw0rd!",
              "nickname": "다른닉네임"
            }
            """;

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
        User existing = User.createUser("user1", "dup@example.com", "password123!");
        existing.setUserProfile(new UserProfile(existing, "nick1", null, null, null, 0));
        userRepository.save(existing);

        String body = """
            {
              "username": "otheruser",
              "email": "dup@example.com",
              "password": "P@ssw0rd!",
              "nickname": "다른닉네임"
            }
            """;

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
        User existing = User.createUser("user2", "user2@example.com", "password123!");
        existing.setUserProfile(new UserProfile(existing, "dupnick", null, null, null, 0));
        userRepository.save(existing);

        String body = """
            {
              "username": "newuser",
              "email": "new@example.com",
              "password": "P@ssw0rd!",
              "nickname": "dupnick"
            }
            """;

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
        String body = """
            {
              "username": "weakpw",
              "email": "weak@example.com",
              "password": "password",
              "nickname": "닉네임"
            }
            """;

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
        String body = """
            {
              "username": "",
              "email": "invalid",
              "password": "",
              "nickname": ""
            }
            """;

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }
}