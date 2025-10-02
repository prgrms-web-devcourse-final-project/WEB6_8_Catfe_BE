package com.back.domain.board.service;

import com.back.domain.board.dto.*;
import com.back.domain.board.entity.Post;
import com.back.domain.board.entity.PostCategory;
import com.back.domain.board.repository.PostCategoryRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    // ====================== 게시글 생성 테스트 ======================

    @Test
    @DisplayName("게시글 생성 성공 - 카테고리 포함")
    void createPost_success_withCategories() {
        // given: 유저 + 카테고리 저장
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory category = new PostCategory("공지");
        postCategoryRepository.save(category);

        PostRequest request = new PostRequest("제목", "내용", List.of(category.getId()));

        // when
        PostResponse response = postService.createPost(request, user.getId());

        // then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        assertThat(response.author().nickname()).isEqualTo("작성자");
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).name()).isEqualTo("공지");
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 유저")
    void createPost_fail_userNotFound() {
        // given
        PostRequest request = new PostRequest("제목", "내용", null);

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, 999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 카테고리 ID 포함")
    void createPost_fail_categoryNotFound() {
        // given: 유저는 정상
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 실제 저장 안 된 카테고리 ID 요청
        PostRequest request = new PostRequest("제목", "내용", List.of(100L, 200L));

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

    // ====================== 게시글 조회 테스트 ======================


    @Test
    @DisplayName("게시글 다건 조회 성공 - 페이징 + 카테고리")
    void getPosts_success() {
        // given
        User user = User.createUser("writer3", "writer3@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자3", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory c1 = new PostCategory("공지사항");
        PostCategory c2 = new PostCategory("자유게시판");
        postCategoryRepository.saveAll(List.of(c1, c2));

        Post post1 = new Post(user, "첫 번째 글", "내용1");
        post1.updateCategories(List.of(c1));
        postRepository.save(post1);

        Post post2 = new Post(user, "두 번째 글", "내용2");
        post2.updateCategories(List.of(c2));
        postRepository.save(post2);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<PostListResponse> response = postService.getPosts(null, null, null, pageable);

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).getTitle()).isEqualTo("두 번째 글");
        assertThat(response.items().get(1).getTitle()).isEqualTo("첫 번째 글");
    }

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void getPost_success() {
        // given
        User user = User.createUser("reader", "reader@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "독자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory category = new PostCategory("공지");
        postCategoryRepository.save(category);

        Post post = new Post(user, "조회용 제목", "조회용 내용");
        post.updateCategories(List.of(category));
        postRepository.save(post);

        // when
        PostDetailResponse response = postService.getPost(post.getId());

        // then
        assertThat(response.postId()).isEqualTo(post.getId());
        assertThat(response.title()).isEqualTo("조회용 제목");
        assertThat(response.content()).isEqualTo("조회용 내용");
        assertThat(response.author().nickname()).isEqualTo("독자");
        assertThat(response.categories()).extracting("name").containsExactly("공지");
        assertThat(response.likeCount()).isZero();
        assertThat(response.bookmarkCount()).isZero();
        assertThat(response.commentCount()).isZero();
    }

    @Test
    @DisplayName("게시글 단건 조회 실패 - 존재하지 않는 게시글")
    void getPost_fail_postNotFound() {
        // when & then
        assertThatThrownBy(() -> postService.getPost(999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }
}
