package com.back.domain.board.controller;

import com.back.domain.board.comment.dto.CommentRequest;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.comment.repository.CommentRepository;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

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

    // ====================== 댓글 생성 테스트 ======================

    @Test
    @DisplayName("댓글 생성 성공 → 201 Created")
    void createComment_success() throws Exception {
        // given: 정상 유저 + 게시글
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "첫 글", "내용");
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        CommentRequest request = new CommentRequest("좋은 글 감사합니다!");

        // when
        ResultActions resultActions = mvc.perform(
                post("/api/posts/{postId}/comments", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andDo(print());

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.content").value("좋은 글 감사합니다!"))
                .andExpect(jsonPath("$.data.author.nickname").value("홍길동"))
                .andExpect(jsonPath("$.data.postId").value(post.getId()));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void createComment_userNotFound() throws Exception {
        // given: 게시글 저장
        User user = User.createUser("temp", "temp@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "임시", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        // DB에 없는 userId 기반 토큰
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        CommentRequest request = new CommentRequest("댓글 내용");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void createComment_postNotFound() throws Exception {
        // given: 정상 유저
        User user = User.createUser("writer2", "writer2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        CommentRequest request = new CommentRequest("댓글 내용");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments", 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 잘못된 요청(필드 누락) → 400 Bad Request")
    void createComment_badRequest() throws Exception {
        // given: 정상 유저 + 게시글
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        // content 누락
        String invalidJson = """
                {
                }
                """;

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 토큰 없음 → 401 Unauthorized")
    void createComment_noToken() throws Exception {
        // given: 게시글
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        CommentRequest request = new CommentRequest("댓글 내용");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    // ====================== 댓글 조회 테스트 ======================

    @Test
    @DisplayName("댓글 목록 조회 성공 → 200 OK")
    void getComments_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        // 부모 댓글
        Comment parent = new Comment(post, user, "부모 댓글", null);
        commentRepository.save(parent);

        // 자식 댓글
        Comment child = new Comment(post, user, "자식 댓글", parent);
        commentRepository.save(child);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(get("/api/posts/{postId}/comments", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.items[0].content").value("부모 댓글"))
                .andExpect(jsonPath("$.data.items[0].children[0].content").value("자식 댓글"));
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void getComments_postNotFound() throws Exception {
        // given
        User user = User.createUser("ghost", "ghost@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "유저", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(get("/api/posts/{postId}/comments", 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    // ====================== 댓글 수정 테스트 ======================

    @Test
    @DisplayName("댓글 수정 성공 → 200 OK")
    void updateComment_success() throws Exception {
        // given: 유저 + 게시글 + 댓글
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "원래 댓글 내용");
        commentRepository.save(comment);

        String accessToken = generateAccessToken(user);

        CommentRequest updateRequest = new CommentRequest("수정된 댓글 내용입니다.");

        // when
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.commentId").value(comment.getId()))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글 내용입니다."))
                .andExpect(jsonPath("$.data.author.nickname").value("홍길동"));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void updateComment_postNotFound() throws Exception {
        // given
        User user = User.createUser("writer2", "writer2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "댓글");
        commentRepository.save(comment);

        String accessToken = generateAccessToken(user);
        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        // when & then
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", 999L, comment.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 존재하지 않는 댓글 → 404 Not Found")
    void updateComment_commentNotFound() throws Exception {
        // given
        User user = User.createUser("writer3", "writer3@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        String accessToken = generateAccessToken(user);
        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        // when & then
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", post.getId(), 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 작성자가 아님 → 403 Forbidden")
    void updateComment_noPermission() throws Exception {
        // given: 작성자와 다른 유저
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User other = User.createUser("other", "other@example.com", passwordEncoder.encode("P@ssw0rd!"));
        other.setUserProfile(new UserProfile(other, "다른사람", null, null, null, 0));
        other.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(other);

        Post post = new Post(writer, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, writer, "원래 댓글");
        commentRepository.save(comment);

        String accessToken = generateAccessToken(other);
        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        // when & then
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_002"))
                .andExpect(jsonPath("$.message").value("댓글 작성자만 수정/삭제할 수 있습니다."));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 잘못된 요청(필드 누락) → 400 Bad Request")
    void updateComment_badRequest() throws Exception {
        // given
        User user = User.createUser("writer4", "writer4@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자4", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "댓글");
        commentRepository.save(comment);

        String accessToken = generateAccessToken(user);

        String invalidJson = """
                {}
                """;

        // when & then
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 토큰 없음 → 401 Unauthorized")
    void updateComment_noToken() throws Exception {
        // given
        User user = User.createUser("writer5", "writer5@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자5", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "댓글");
        commentRepository.save(comment);

        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        // when & then
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    // ====================== 댓글 삭제 테스트 ======================

    @Test
    @DisplayName("댓글 삭제 성공 → 200 OK")
    void deleteComment_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "삭제할 댓글");
        commentRepository.save(comment);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("댓글이 삭제되었습니다."));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void deleteComment_postNotFound() throws Exception {
        // given
        User user = User.createUser("writer2", "writer2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "댓글");
        commentRepository.save(comment);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", 999L, comment.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글 → 404 Not Found")
    void deleteComment_commentNotFound() throws Exception {
        // given
        User user = User.createUser("writer3", "writer3@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자3", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), 999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자가 아님 → 403 Forbidden")
    void deleteComment_noPermission() throws Exception {
        // given
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User other = User.createUser("other", "other@example.com", passwordEncoder.encode("P@ssw0rd!"));
        other.setUserProfile(new UserProfile(other, "다른사람", null, null, null, 0));
        other.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(other);

        Post post = new Post(writer, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, writer, "원래 댓글");
        commentRepository.save(comment);

        String accessToken = generateAccessToken(other);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_002"))
                .andExpect(jsonPath("$.message").value("댓글 작성자만 수정/삭제할 수 있습니다."));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 토큰 없음 → 401 Unauthorized")
    void deleteComment_noToken() throws Exception {
        // given
        User user = User.createUser("writer4", "writer4@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자4", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "댓글");
        commentRepository.save(comment);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), comment.getId()))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    // ====================== 대댓글 테스트 ======================

    @Test
    @DisplayName("대댓글 생성 성공 → 201 Created")
    void createReply_success() throws Exception {
        // given: 유저 + 게시글 + 부모 댓글
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "첫 글", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        commentRepository.save(parent);

        String accessToken = generateAccessToken(user);

        CommentRequest request = new CommentRequest("저도 동의합니다!");

        // when
        ResultActions resultActions = mvc.perform(
                post("/api/posts/{postId}/comments/{commentId}/replies", post.getId(), parent.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andDo(print());

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.data.content").value("저도 동의합니다!"))
                .andExpect(jsonPath("$.data.author.nickname").value("이몽룡"))
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.parentId").value(parent.getId()));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void createReply_userNotFound() throws Exception {
        // given: 게시글 + 부모 댓글
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        commentRepository.save(parent);

        // DB에 없는 userId로 JWT 발급
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost@example.com", "USER");

        CommentRequest request = new CommentRequest("대댓글 내용");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/replies", post.getId(), parent.getId())
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void createReply_postNotFound() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        commentRepository.save(parent);

        String accessToken = generateAccessToken(user);
        CommentRequest request = new CommentRequest("대댓글 내용");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/replies", 999L, parent.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 존재하지 않는 부모 댓글 → 404 Not Found")
    void createReply_commentNotFound() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        String accessToken = generateAccessToken(user);
        CommentRequest request = new CommentRequest("대댓글 내용");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/replies", post.getId(), 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 부모 댓글이 다른 게시글에 속함 → 400 Bad Request")
    void createReply_parentMismatch() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post1 = new Post(user, "게시글1", "내용1");
        Post post2 = new Post(user, "게시글2", "내용2");
        postRepository.saveAll(List.of(post1, post2));

        Comment parent = new Comment(post1, user, "부모 댓글", null);
        commentRepository.save(parent);

        String accessToken = generateAccessToken(user);
        CommentRequest request = new CommentRequest("대댓글 내용");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/replies", post2.getId(), parent.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENT_003"))
                .andExpect(jsonPath("$.message").value("부모 댓글이 해당 게시글에 속하지 않습니다."));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - depth 제한 초과(부모가 이미 대댓글) → 400 Bad Request")
    void createReply_depthExceeded() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        Comment child = new Comment(post, user, "대댓글1", parent);
        commentRepository.saveAll(List.of(parent, child));

        String accessToken = generateAccessToken(user);
        CommentRequest request = new CommentRequest("대댓글2");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/replies", post.getId(), child.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENT_004"))
                .andExpect(jsonPath("$.message").value("대댓글은 한 단계까지만 작성할 수 있습니다."));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 필드 누락(content 없음) → 400 Bad Request")
    void createReply_badRequest() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        commentRepository.save(parent);

        String accessToken = generateAccessToken(user);

        String invalidJson = "{}";

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/replies", post.getId(), parent.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 토큰 없음 → 401 Unauthorized")
    void createReply_noToken() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        commentRepository.save(parent);

        CommentRequest request = new CommentRequest("대댓글 내용");

        // when & then
        mvc.perform(post("/api/posts/{postId}/comments/{commentId}/replies", post.getId(), parent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("대댓글 수정 성공 → 200 OK")
    void updateReply_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        Comment reply = new Comment(post, user, "대댓글", parent);
        commentRepository.saveAll(List.of(parent, reply));

        String accessToken = generateAccessToken(user);
        CommentRequest request = new CommentRequest("수정된 대댓글 내용");

        // when & then
        mvc.perform(put("/api/posts/{postId}/comments/{commentId}", post.getId(), reply.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 대댓글 내용"));
    }

    @Test
    @DisplayName("대댓글 삭제 성공 → 200 OK")
    void deleteReply_success() throws Exception {
        // given
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "이몽룡", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        Comment reply = new Comment(post, user, "대댓글", parent);
        commentRepository.saveAll(List.of(parent, reply));

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/posts/{postId}/comments/{commentId}", post.getId(), reply.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 삭제되었습니다."));
    }
}
