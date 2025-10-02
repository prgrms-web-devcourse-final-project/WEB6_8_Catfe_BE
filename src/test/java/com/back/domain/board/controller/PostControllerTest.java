package com.back.domain.board.controller;

import com.back.domain.board.dto.PostRequest;
import com.back.domain.board.entity.PostCategory;
import com.back.domain.board.repository.PostCategoryRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    // ====================== 게시글 생성 테스트 ======================

    @Test
    @DisplayName("게시글 생성 성공 → 201 Created")
    void createPost_success() throws Exception {
        // given: 정상 유저 생성
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 카테고리 등록
        PostCategory c1 = new PostCategory("공지사항");
        postCategoryRepository.save(c1);

        PostCategory c2 = new PostCategory("자유게시판");
        postCategoryRepository.save(c2);

        PostRequest request = new PostRequest("첫 번째 게시글", "안녕하세요, 첫 글입니다!", List.of(c1.getId(), c2.getId()));

        // when
        ResultActions resultActions = mvc.perform(
                post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andDo(print());

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.title").value("첫 번째 게시글"))
                .andExpect(jsonPath("$.data.author.nickname").value("홍길동"))
                .andExpect(jsonPath("$.data.categories.length()").value(2));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 → 404 Not Found")
    void createPost_userNotFound() throws Exception {
        // given: 토큰만 발급(실제 DB엔 없음)
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        PostRequest request = new PostRequest("제목", "내용", null);

        // when & then
        mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 → 404 Not Found")
    void createPost_categoryNotFound() throws Exception {
        // given: 정상 유저
        User user = User.createUser("writer2", "writer2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // 존재하지 않는 카테고리 ID
        PostRequest request = new PostRequest("제목", "내용", List.of(999L));

        // when & then
        mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_003"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 카테고리입니다."));
    }

    @Test
    @DisplayName("잘못된 요청(필드 누락) → 400 Bad Request")
    void createPost_badRequest() throws Exception {
        // given: 정상 유저 생성
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // given: title 누락
        String invalidJson = """
                {
                  "content": "본문만 있음"
                }
                """;

        // when & then
        mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }
}
