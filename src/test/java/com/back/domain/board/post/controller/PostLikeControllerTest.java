package com.back.domain.board.post.controller;

import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.repository.PostRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostLikeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    // ====================== 게시글 좋아요 등록 ======================

    @Test
    @DisplayName("게시글 좋아요 등록 성공 → 200 OK")
    void likePost_success() throws Exception {
        // given
        User user = User.createUser("likeUser", "like@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "좋아요 테스트", "내용입니다", null);
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        // when
        mvc.perform(post("/api/posts/{postId}/like", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("게시글 좋아요가 등록되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("게시글 좋아요 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void likePost_userNotFound() throws Exception {
        // given
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        // 실제 게시글은 존재
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        Post post = new Post(writer, "게시글", "내용", null);
        postRepository.save(post);

        // when & then
        mvc.perform(post("/api/posts/{postId}/like", post.getId())
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("게시글 좋아요 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void likePost_postNotFound() throws Exception {
        // given
        User user = User.createUser("likeUser2", "like2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(post("/api/posts/{postId}/like", 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 좋아요 실패 - 이미 좋아요 누름 → 409 Conflict")
    void likePost_alreadyLiked() throws Exception {
        // given
        User user = User.createUser("likeUser3", "like3@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "테스트 게시글", "내용", null);
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        // 먼저 좋아요 1회
        mvc.perform(post("/api/posts/{postId}/like", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 같은 요청 반복
        mvc.perform(post("/api/posts/{postId}/like", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POST_005"))
                .andExpect(jsonPath("$.message").value("이미 좋아요한 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 좋아요 실패 - 토큰 없음 → 401 Unauthorized")
    void likePost_noToken() throws Exception {
        // given
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        Post post = new Post(writer, "테스트", "내용", null);
        postRepository.save(post);

        // when & then
        mvc.perform(post("/api/posts/{postId}/like", post.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    // ====================== 게시글 좋아요 취소 ======================

    @Test
    @DisplayName("게시글 좋아요 취소 성공 → 200 OK")
    void cancelLikePost_success() throws Exception {
        // given
        User user = User.createUser("cancelUser", "cancel@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "좋아요 취소 테스트", "내용입니다", null);
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        // 먼저 좋아요 등록
        mvc.perform(post("/api/posts/{postId}/like", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // when: 좋아요 취소
        mvc.perform(delete("/api/posts/{postId}/like", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("게시글 좋아요가 취소되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("게시글 좋아요 취소 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void cancelLikePost_userNotFound() throws Exception {
        // given
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        // 실제 게시글 존재
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        Post post = new Post(writer, "게시글", "내용", null);
        postRepository.save(post);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/like", post.getId())
                        .header("Authorization", "Bearer " + fakeToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("게시글 좋아요 취소 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void cancelLikePost_postNotFound() throws Exception {
        // given
        User user = User.createUser("cancelUser2", "cancel2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/like", 999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 좋아요 취소 실패 - 좋아요 기록 없음 → 404 Not Found")
    void cancelLikePost_notFound() throws Exception {
        // given
        User user = User.createUser("cancelUser3", "cancel3@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "테스트 게시글", "내용", null);
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/like", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_006"))
                .andExpect(jsonPath("$.message").value("해당 게시글에 대한 좋아요 기록이 없습니다."));
    }

    @Test
    @DisplayName("게시글 좋아요 취소 실패 - 토큰 없음 → 401 Unauthorized")
    void cancelLikePost_noToken() throws Exception {
        // given
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        Post post = new Post(writer, "테스트", "내용", null);
        postRepository.save(post);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/like", post.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }
}
