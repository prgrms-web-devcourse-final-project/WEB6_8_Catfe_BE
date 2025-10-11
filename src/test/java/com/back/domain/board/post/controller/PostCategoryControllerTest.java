package com.back.domain.board.post.controller;

import com.back.domain.board.post.dto.CategoryRequest;
import com.back.domain.board.post.enums.CategoryType;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.fixture.TestJwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostCategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    // ====================== 카테고리 생성 테스트 ======================

    @Test
    @DisplayName("카테고리 생성 성공 → 201 Created")
    void createCategory_success() throws Exception {
        // given: 로그인 사용자
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        CategoryRequest request = new CategoryRequest("수학 II", CategoryType.SUBJECT);

        // when
        ResultActions resultActions = mvc.perform(post("/api/posts/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.name").value("수학 II"))
                .andExpect(jsonPath("$.data.type").value("SUBJECT"));

        // DB 확인
        assertThat(postCategoryRepository.existsByName("수학 II")).isTrue();
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void createCategory_userNotFound() throws Exception {
        // given: 토큰만 존재 (DB엔 유저 없음)
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");
        CategoryRequest request = new CategoryRequest("영어", CategoryType.SUBJECT);

        // when & then
        mvc.perform(post("/api/posts/categories")
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 이미 존재하는 카테고리 → 409 Conflict")
    void createCategory_conflict() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 기존 카테고리 저장
        postCategoryRepository.save(new PostCategory("CS", CategoryType.SUBJECT));

        CategoryRequest request = new CategoryRequest("CS", CategoryType.SUBJECT);

        // when & then
        mvc.perform(post("/api/posts/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POST_004"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 카테고리입니다."));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 요청 필드 누락 → 400 Bad Request")
    void createCategory_badRequest() throws Exception {
        // given: 로그인 유저
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // name 누락
        String invalidJson = """
                {
                  "type": "SUBJECT"
                }
                """;

        // when & then
        mvc.perform(post("/api/posts/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 토큰 없음 → 401 Unauthorized")
    void createCategory_noToken() throws Exception {
        // given
        CategoryRequest request = new CategoryRequest("프론트엔드", CategoryType.SUBJECT);

        // when & then
        mvc.perform(post("/api/posts/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    // ====================== 카테고리 전체 조회 테스트 ======================

    @Test
    @DisplayName("카테고리 전체 조회 성공 → 200 OK")
    void getAllCategories_success() throws Exception {
        // given
        postCategoryRepository.saveAll(List.of(
                new PostCategory("백엔드", CategoryType.SUBJECT),
                new PostCategory("중학생", CategoryType.DEMOGRAPHIC),
                new PostCategory("2~4명", CategoryType.GROUP_SIZE)
        ));

        // when
        ResultActions resultActions = mvc.perform(get("/api/posts/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].type").exists());
    }
}
