package com.back.domain.board.post.service;

import com.back.domain.board.post.dto.PostLikeResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostLike;
import com.back.domain.board.post.repository.PostLikeRepository;
import com.back.domain.board.post.repository.PostRepository;
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
class PostLikeServiceTest {

    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private UserRepository userRepository;

    // ====================== 게시글 좋아요 테스트 ======================

    @Test
    @DisplayName("게시글 좋아요 성공")
    void likePost_success() {
        // given
        User user = User.createUser("user1", "user1@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        // when
        PostLikeResponse response = postLikeService.likePost(post.getId(), user.getId());

        // then
        assertThat(response.likeCount()).isEqualTo(1);
        assertThat(postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isTrue();
    }

    @Test
    @DisplayName("게시글 좋아요 실패 - 존재하지 않는 게시글")
    void likePost_fail_postNotFound() {
        // given
        User user = User.createUser("user2", "user2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> postLikeService.likePost(999L, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 좋아요 실패 - 이미 좋아요한 경우")
    void likePost_fail_alreadyLiked() {
        // given
        User user = User.createUser("user3", "user3@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        postLikeRepository.save(new PostLike(post, user));

        // when & then
        assertThatThrownBy(() -> postLikeService.likePost(post.getId(), user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_ALREADY_LIKED.getMessage());
    }

    // ====================== 게시글 좋아요 취소 테스트 ======================

    @Test
    @DisplayName("게시글 좋아요 취소 성공")
    void cancelLikePost_success() {
        // given
        User user = User.createUser("user4", "user4@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        PostLike postLike = new PostLike(post, user);
        postLikeRepository.save(postLike);
        post.increaseLikeCount();

        // when
        PostLikeResponse response = postLikeService.cancelLikePost(post.getId(), user.getId());

        // then
        assertThat(response.likeCount()).isEqualTo(0);
        assertThat(postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isFalse();
    }

    @Test
    @DisplayName("게시글 좋아요 취소 실패 - 좋아요 내역 없음")
    void cancelLikePost_fail_notFound() {
        // given
        User user = User.createUser("user5", "user5@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        // when & then
        assertThatThrownBy(() -> postLikeService.cancelLikePost(post.getId(), user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_LIKE_NOT_FOUND.getMessage());
    }
}
