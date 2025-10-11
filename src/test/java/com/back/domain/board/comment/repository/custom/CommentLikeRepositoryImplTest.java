package com.back.domain.board.comment.repository.custom;

import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.entity.CommentLike;
import com.back.domain.board.comment.repository.CommentLikeRepository;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.global.config.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class CommentLikeRepositoryImplTest {

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Post post;
    private Comment c1;
    private Comment c2;
    private Comment c3;

    @BeforeEach
    void setUp() {
        // 사용자 저장
        user = User.createUser("user1", "user1@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 게시글 + 댓글 3개 생성
        post = new Post(user, "게시글 제목", "내용", null);
        postRepository.save(post);

        c1 = Comment.createRoot(post, user, "댓글1");
        c2 = Comment.createRoot(post, user, "댓글2");
        c3 = Comment.createRoot(post, user, "댓글3");
        commentRepository.saveAll(List.of(c1, c2, c3));

        // 사용자가 c1, c3 좋아요
        commentLikeRepository.save(new CommentLike(c1, user));
        commentLikeRepository.save(new CommentLike(c3, user));
    }

    @Test
    @DisplayName("findLikedCommentIdsIn - 사용자가 좋아요한 댓글 ID만 반환한다")
    void findLikedCommentIdsIn_success() {
        // when
        List<Long> likedIds = commentLikeRepository.findLikedCommentIdsIn(
                user.getId(),
                List.of(c1.getId(), c2.getId(), c3.getId())
        );

        // then
        assertThat(likedIds)
                .containsExactlyInAnyOrder(c1.getId(), c3.getId())
                .doesNotContain(c2.getId());
    }

    @Test
    @DisplayName("findLikedCommentIdsIn - 좋아요한 댓글이 없는 경우 빈 리스트 반환")
    void findLikedCommentIdsIn_emptyResult() {
        // given
        User anotherUser = User.createUser("user2", "user2@example.com", "encodedPwd");
        anotherUser.setUserProfile(new UserProfile(anotherUser, "작성자2", null, null, null, 0));
        anotherUser.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(anotherUser);

        // when
        List<Long> likedIds = commentLikeRepository.findLikedCommentIdsIn(
                anotherUser.getId(),
                List.of(c1.getId(), c2.getId(), c3.getId())
        );

        // then
        assertThat(likedIds).isEmpty();
    }

    @Test
    @DisplayName("findLikedCommentIdsIn - commentIds가 비어 있으면 빈 리스트 반환")
    void findLikedCommentIdsIn_emptyInput() {
        // when
        List<Long> likedIds = commentLikeRepository.findLikedCommentIdsIn(user.getId(), List.of());

        // then
        assertThat(likedIds).isEmpty();
    }
}
