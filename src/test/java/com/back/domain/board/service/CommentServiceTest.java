package com.back.domain.board.service;

import com.back.domain.board.dto.CommentRequest;
import com.back.domain.board.dto.CommentResponse;
import com.back.domain.board.entity.Post;
import com.back.domain.board.repository.CommentRepository;
import com.back.domain.board.repository.PostRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    // ====================== 댓글 생성 테스트 ======================

    @Test
    @DisplayName("댓글 생성 성공")
    void createComment_success() {
        // given: 유저 + 게시글 저장
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        CommentRequest request = new CommentRequest("댓글 내용");

        // when
        CommentResponse response = commentService.createComment(post.getId(), request, user.getId());

        // then
        assertThat(response.content()).isEqualTo("댓글 내용");
        assertThat(response.author().nickname()).isEqualTo("작성자");
        assertThat(response.postId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("댓글 생성 실패 - 존재하지 않는 유저")
    void createComment_fail_userNotFound() {
        // given: 게시글 저장
        User user = User.createUser("temp", "temp@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "임시유저", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        CommentRequest request = new CommentRequest("댓글 내용");

        // when & then
        assertThatThrownBy(() -> commentService.createComment(post.getId(), request, 999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 생성 실패 - 존재하지 않는 게시글")
    void createComment_fail_postNotFound() {
        // given: 유저 저장
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        CommentRequest request = new CommentRequest("댓글 내용");

        // when & then
        assertThatThrownBy(() -> commentService.createComment(999L, request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }
}
