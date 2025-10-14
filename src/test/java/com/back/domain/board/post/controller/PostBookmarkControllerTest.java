package com.back.domain.board.post.controller;

import com.back.domain.board.post.entity.Post;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import com.back.domain.board.post.repository.PostRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostBookmarkControllerTest {

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

    // ====================== 게시글 북마크 등록 ======================

    @Test
    @DisplayName("게시글 북마크 등록 성공 → 200 OK")
    void bookmarkPost_success() throws Exception {
        User user = User.createUser("bookmarkUser", "bookmark@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "북마크 테스트", "내용입니다", null);
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        mvc.perform(post("/api/posts/{postId}/bookmark", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("게시글 북마크가 등록되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.bookmarkCount").value(1));
    }

    @Test
    @DisplayName("게시글 북마크 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void bookmarkPost_userNotFound() throws Exception {
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        Post post = new Post(writer, "게시글", "내용", null);
        postRepository.save(post);

        mvc.perform(post("/api/posts/{postId}/bookmark", post.getId())
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("게시글 북마크 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void bookmarkPost_postNotFound() throws Exception {
        User user = User.createUser("bookmarkUser2", "bookmark2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        String accessToken = generateAccessToken(user);

        mvc.perform(post("/api/posts/{postId}/bookmark", 999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 북마크 실패 - 이미 북마크 등록 → 409 Conflict")
    void bookmarkPost_alreadyBookmarked() throws Exception {
        User user = User.createUser("bookmarkUser3", "bookmark3@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "테스트 게시글", "내용", null);
        postRepository.save(post);

        String accessToken = generateAccessToken(user);

        mvc.perform(post("/api/posts/{postId}/bookmark", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mvc.perform(post("/api/posts/{postId}/bookmark", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POST_007"))
                .andExpect(jsonPath("$.message").value("이미 북마크한 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 북마크 실패 - 토큰 없음 → 401 Unauthorized")
    void bookmarkPost_noToken() throws Exception {
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        Post post = new Post(writer, "테스트", "내용", null);
        postRepository.save(post);

        mvc.perform(post("/api/posts/{postId}/bookmark", post.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("게시글 북마크 실패 - 잘못된 요청 파라미터 → 400 Bad Request")
    void bookmarkPost_badRequest() throws Exception {
        User user = User.createUser("bookmarkUser", "bookmark@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        mvc.perform(post("/api/posts/{postId}/bookmark", "invalid") // invalid param
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    // ====================== 게시글 북마크 취소 ======================

    @Test
    @DisplayName("게시글 북마크 취소 성공 → 200 OK")
    void cancelBookmarkPost_success() throws Exception {
        User user = User.createUser("cancelUser", "cancel@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "북마크 취소 테스트", "내용", null);
        postRepository.save(post);
        String accessToken = generateAccessToken(user);

        mvc.perform(post("/api/posts/{postId}/bookmark", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/posts/{postId}/bookmark", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("게시글 북마크가 취소되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.bookmarkCount").value(0));
    }

    @Test
    @DisplayName("게시글 북마크 취소 실패 - 존재하지 않는 사용자 → 404 Not Found")
    void cancelBookmarkPost_userNotFound() throws Exception {
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        Post post = new Post(writer, "게시글", "내용", null);
        postRepository.save(post);

        mvc.perform(delete("/api/posts/{postId}/bookmark", post.getId())
                        .header("Authorization", "Bearer " + fakeToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("게시글 북마크 취소 실패 - 존재하지 않는 게시글 → 404 Not Found")
    void cancelBookmarkPost_postNotFound() throws Exception {
        User user = User.createUser("cancelUser2", "cancel2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        String accessToken = generateAccessToken(user);

        mvc.perform(delete("/api/posts/{postId}/bookmark", 999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("게시글 북마크 취소 실패 - 북마크 내역 없음 → 404 Not Found")
    void cancelBookmarkPost_notFound() throws Exception {
        User user = User.createUser("cancelUser3", "cancel3@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "테스트", "내용", null);
        postRepository.save(post);
        String accessToken = generateAccessToken(user);

        mvc.perform(delete("/api/posts/{postId}/bookmark", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_008"))
                .andExpect(jsonPath("$.message").value("해당 게시글에 대한 북마크 기록이 없습니다."));
    }

    @Test
    @DisplayName("게시글 북마크 취소 실패 - 토큰 없음 → 401 Unauthorized")
    void cancelBookmarkPost_noToken() throws Exception {
        User writer = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        Post post = new Post(writer, "테스트", "내용", null);
        postRepository.save(post);

        mvc.perform(delete("/api/posts/{postId}/bookmark", post.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("게시글 북마크 취소 실패 - 잘못된 요청 파라미터 → 400 Bad Request")
    void cancelBookmarkPost_badRequest() throws Exception {
        User user = User.createUser("bookmarkUser", "bookmark@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        mvc.perform(delete("/api/posts/{postId}/bookmark", "invalid") // invalid param
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }
}
