package com.back.domain.board.post.service;

import com.back.domain.board.post.dto.PostBookmarkResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostBookmark;
import com.back.domain.board.post.repository.PostBookmarkRepository;
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
class PostBookmarkServiceTest {

    @Autowired
    private PostBookmarkService postBookmarkService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    // ====================== 게시글 북마크 등록 테스트 ======================

    @Test
    @DisplayName("게시글 북마크 성공")
    void bookmarkPost_success() {
        // given
        User user = User.createUser("user1", "user1@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용", null);
        postRepository.save(post);

        // when
        PostBookmarkResponse response = postBookmarkService.bookmarkPost(post.getId(), user.getId());

        // then
        assertThat(response.bookmarkCount()).isEqualTo(1);
        assertThat(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isTrue();
    }

    @Test
    @DisplayName("게시글 북마크 실패 - 존재하지 않는 게시글")
    void bookmarkPost_fail_postNotFound() {
        // given
        User user = User.createUser("user2", "user2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> postBookmarkService.bookmarkPost(999L, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 북마크 실패 - 이미 북마크한 경우")
    void bookmarkPost_fail_alreadyBookmarked() {
        // given
        User user = User.createUser("user3", "user3@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용", null);
        postRepository.save(post);

        postBookmarkRepository.save(new PostBookmark(post, user));

        // when & then
        assertThatThrownBy(() -> postBookmarkService.bookmarkPost(post.getId(), user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.BOOKMARK_ALREADY_EXISTS.getMessage());
    }

    // ====================== 게시글 북마크 취소 테스트 ======================

    @Test
    @DisplayName("게시글 북마크 취소 성공")
    void cancelBookmarkPost_success() {
        // given
        User user = User.createUser("user4", "user4@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용", null);
        postRepository.save(post);

        PostBookmark postBookmark = new PostBookmark(post, user);
        postBookmarkRepository.save(postBookmark);
        post.increaseBookmarkCount();

        // when
        PostBookmarkResponse response = postBookmarkService.cancelBookmarkPost(post.getId(), user.getId());

        // then
        assertThat(response.bookmarkCount()).isEqualTo(0);
        assertThat(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), post.getId())).isFalse();
    }

    @Test
    @DisplayName("게시글 북마크 취소 실패 - 북마크 내역 없음")
    void cancelBookmarkPost_fail_notFound() {
        // given
        User user = User.createUser("user5", "user5@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용", null);
        postRepository.save(post);

        // when & then
        assertThatThrownBy(() -> postBookmarkService.cancelBookmarkPost(post.getId(), user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.BOOKMARK_NOT_FOUND.getMessage());
    }
}
