package com.back.domain.board.service;

import com.back.domain.board.comment.dto.CommentLikeResponse;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.repository.CommentLikeRepository;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.comment.service.CommentLikeService;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CommentLikeServiceTest {

    @Autowired
    private CommentLikeService commentLikeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    private User user;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = User.createUser("user1", "user1@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        post = new Post(user, "게시글 제목", "게시글 내용");
        postRepository.save(post);

        comment = new Comment(post, user, "댓글 내용");
        commentRepository.save(comment);
    }

    // ====================== 좋아요 추가 테스트 ======================

    @Test
    @DisplayName("댓글 좋아요 성공")
    void likeComment_success() {
        // when
        CommentLikeResponse response = commentLikeService.likeComment(comment.getId(), user.getId());

        // then
        assertThat(response.commentId()).isEqualTo(comment.getId());
        assertThat(response.likeCount()).isEqualTo(1L);
        assertThat(commentLikeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId())).isTrue();
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 이미 좋아요한 댓글")
    void likeComment_fail_alreadyLiked() {
        // given
        commentLikeService.likeComment(comment.getId(), user.getId());

        // when & then
        assertThatThrownBy(() -> commentLikeService.likeComment(comment.getId(), user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_ALREADY_LIKED.getMessage());
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 존재하지 않는 댓글")
    void likeComment_fail_commentNotFound() {
        // when & then
        assertThatThrownBy(() -> commentLikeService.likeComment(999L, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 좋아요 실패 - 존재하지 않는 유저")
    void likeComment_fail_userNotFound() {
        // when & then
        assertThatThrownBy(() -> commentLikeService.likeComment(comment.getId(), 999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // ====================== 좋아요 취소 테스트 ======================

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void cancelLikeComment_success() {
        // given
        commentLikeService.likeComment(comment.getId(), user.getId());

        // when
        CommentLikeResponse response = commentLikeService.cancelLikeComment(comment.getId(), user.getId());

        // then
        assertThat(response.commentId()).isEqualTo(comment.getId());
        assertThat(response.likeCount()).isEqualTo(0L);
        assertThat(commentLikeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId())).isFalse();
    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 좋아요하지 않은 댓글")
    void cancelLikeComment_fail_notLiked() {
        // when & then
        assertThatThrownBy(() -> commentLikeService.cancelLikeComment(comment.getId(), user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_LIKE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 존재하지 않는 댓글")
    void cancelLikeComment_fail_commentNotFound() {
        // when & then
        assertThatThrownBy(() -> commentLikeService.cancelLikeComment(999L, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 존재하지 않는 유저")
    void cancelLikeComment_fail_userNotFound() {
        // given
        commentLikeService.likeComment(comment.getId(), user.getId());

        // when & then
        assertThatThrownBy(() -> commentLikeService.cancelLikeComment(comment.getId(), 999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }
}
