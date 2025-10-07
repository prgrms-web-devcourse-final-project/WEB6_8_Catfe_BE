package com.back.domain.board.controller;

import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.repository.CommentLikeRepository;
import com.back.domain.board.comment.repository.CommentRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentLikeControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CommentLikeRepository commentLikeRepository;
    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    private User createUser(String username, String email) {
        User user = User.createUser(username, email, passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, username, null, "소개", LocalDate.of(2000, 1, 1), 0));
        user.setUserStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Post createPost(User user) {
        Post post = new Post(user, "게시글 제목", "게시글 내용");
        return postRepository.save(post);
    }

    // ====================== 댓글 좋아요 등록 ======================

    @Test
    @DisplayName("댓글 좋아요 등록 성공 → 201 Created")
    void likeComment_success() throws Exception {
        User user = createUser("writer", "writer@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글 내용"));

        String token = generateAccessToken(user);

        ResultActions result = mvc.perform(
                post("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print());

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("댓글 좋아요가 등록되었습니다."))
                .andExpect(jsonPath("$.data.commentId").value(comment.getId()))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        assertThat(commentLikeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId())).isTrue();
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void likeComment_fail_userNotFound() throws Exception {
        User user = createUser("temp", "temp@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));

        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 존재하지 않는 댓글 → 404 Not Found")
    void likeComment_fail_commentNotFound() throws Exception {
        User user = createUser("temp", "temp@example.com");
        Post post = createPost(user);
        String token = generateAccessToken(user);

        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/like", post.getId(), 999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 이미 좋아요 누름 → 409 Conflict")
    void likeComment_fail_alreadyLiked() throws Exception {
        User user = createUser("user", "user@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));
        String token = generateAccessToken(user);

        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMENT_005"))
                .andExpect(jsonPath("$.message").value("이미 좋아요한 댓글입니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 잘못된 요청(파라미터 누락) → 400 Bad Request")
    void likeComment_fail_badRequest() throws Exception {
        User user = createUser("user", "user@example.com");
        Post post = createPost(user);
        String token = generateAccessToken(user);

        mvc.perform(post("/api/posts/{postId}/comments//like", post.getId()) // commentId 누락
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 인증 실패 (토큰 없음) → 401 Unauthorized")
    void likeComment_fail_noToken() throws Exception {
        User user = createUser("user", "user@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));

        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 잘못된 토큰 → 401 Unauthorized")
    void likeComment_fail_invalidToken() throws Exception {
        User user = createUser("user", "user@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));

        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    // ====================== 댓글 좋아요 취소 ======================

    @Test
    @DisplayName("댓글 좋아요 취소 성공 → 200 OK")
    void cancelLikeComment_success() throws Exception {
        // given
        User user = createUser("cancel", "cancel@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));
        String token = generateAccessToken(user);

        // 좋아요 등록
        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // when
        ResultActions result = mvc.perform(delete("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("댓글 좋아요가 취소되었습니다."))
                .andExpect(jsonPath("$.data.commentId").value(comment.getId()))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        assertThat(commentLikeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId())).isFalse();
    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 좋아요 기록 없음 → 404 Not Found")
    void cancelLikeComment_fail_notLiked() throws Exception {
        // given
        User user = createUser("user2", "user2@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));
        String token = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMENT_006"))
                .andExpect(jsonPath("$.message").value("해당 댓글에 대한 좋아요 기록이 없습니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void cancelLikeComment_fail_userNotFound() throws Exception {
        // given
        User user = createUser("temp", "temp@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 존재하지 않는 댓글 → 404 Not Found")
    void cancelLikeComment_fail_commentNotFound() throws Exception {
        // given
        User user = createUser("writer", "writer@example.com");
        Post post = createPost(user);
        String token = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}/like", post.getId(), 999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMENT_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 인증 실패 (토큰 없음) → 401 Unauthorized")
    void cancelLikeComment_fail_noToken() throws Exception {
        // given
        User user = createUser("writer", "writer@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 잘못된 토큰 → 401 Unauthorized")
    void cancelLikeComment_fail_invalidToken() throws Exception {
        // given
        User user = createUser("writer", "writer@example.com");
        Post post = createPost(user);
        Comment comment = commentRepository.save(new Comment(post, user, "댓글"));

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}/like", post.getId(), comment.getId())
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }
}
