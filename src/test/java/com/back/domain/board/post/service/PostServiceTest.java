package com.back.domain.board.post.service;

import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.dto.PostDetailResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.dto.PostRequest;
import com.back.domain.board.post.dto.PostResponse;
import com.back.domain.board.post.enums.CategoryType;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
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

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        PostRequest request = new PostRequest("제목", "내용", null, List.of(category.getId()));

        // when
        PostResponse response = postService.createPost(request, user.getId());

        // then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        assertThat(response.author().nickname()).isEqualTo("작성자");
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().getFirst().name()).isEqualTo("공지");
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 유저")
    void createPost_fail_userNotFound() {
        // given
        PostRequest request = new PostRequest("제목", "내용", null, null);

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
        PostRequest request = new PostRequest("제목", "내용", null, List.of(100L, 200L));

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

        PostCategory c1 = new PostCategory("공지사항", CategoryType.SUBJECT);
        PostCategory c2 = new PostCategory("자유게시판", CategoryType.SUBJECT);
        postCategoryRepository.saveAll(List.of(c1, c2));

        Post post1 = new Post(user, "첫 번째 글", "내용1", null);
        post1.updateCategories(List.of(c1));
        postRepository.save(post1);

        Post post2 = new Post(user, "두 번째 글", "내용2", null);
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

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        Post post = new Post(user, "조회용 제목", "조회용 내용", null);
        post.updateCategories(List.of(category));
        postRepository.save(post);

        // when
        PostDetailResponse response = postService.getPost(post.getId(), null);

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
        assertThatThrownBy(() -> postService.getPost(999L, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    // ====================== 게시글 수정 테스트 ======================

    @Test
    @DisplayName("게시글 수정 성공 - 작성자 본인")
    void updatePost_success() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory oldCategory = new PostCategory("공지", CategoryType.SUBJECT);
        PostCategory newCategory = new PostCategory("자유", CategoryType.SUBJECT);
        postCategoryRepository.saveAll(List.of(oldCategory, newCategory));

        Post post = new Post(user, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(oldCategory));
        postRepository.save(post);

        PostRequest request = new PostRequest("수정된 제목", "수정된 내용", null, List.of(newCategory.getId()));

        // when
        PostResponse response = postService.updatePost(post.getId(), request, user.getId());

        // then
        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().getFirst().name()).isEqualTo("자유");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 게시글 없음")
    void updatePost_fail_postNotFound() {
        // given
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostRequest request = new PostRequest("제목", "내용", null, List.of());

        // when & then
        assertThatThrownBy(() -> postService.updatePost(999L, request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 작성자 아님")
    void updatePost_fail_noPermission() {
        // given: 게시글 작성자
        User writer = User.createUser("writer3", "writer3@example.com", "encodedPwd");
        writer.setUserProfile(new UserProfile(writer, "작성자3", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        // 다른 사용자
        User another = User.createUser("other", "other@example.com", "encodedPwd");
        another.setUserProfile(new UserProfile(another, "다른사람", null, null, null, 0));
        another.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(another);

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        Post post = new Post(writer, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(category));
        postRepository.save(post);

        PostRequest request = new PostRequest("수정된 제목", "수정된 내용", null, List.of(category.getId()));

        // when & then
        assertThatThrownBy(() -> postService.updatePost(post.getId(), request, another.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NO_PERMISSION.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않는 카테고리 포함")
    void updatePost_fail_categoryNotFound() {
        // given
        User user = User.createUser("writer4", "writer4@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자4", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        PostCategory category = new PostCategory("공지", CategoryType.SUBJECT);
        postCategoryRepository.save(category);

        Post post = new Post(user, "원래 제목", "원래 내용", null);
        post.updateCategories(List.of(category));
        postRepository.save(post);

        // 실제 DB에는 없는 카테고리 ID 전달
        PostRequest request = new PostRequest("수정된 제목", "수정된 내용", null, List.of(999L));

        // when & then
        assertThatThrownBy(() -> postService.updatePost(post.getId(), request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

    // ====================== 게시글 삭제 테스트 ======================

    @Test
    @DisplayName("게시글 삭제 성공 - 작성자 본인")
    void deletePost_success() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "삭제 대상 제목", "삭제 대상 내용", null);
        postRepository.save(post);

        // when
        postService.deletePost(post.getId(), user.getId());

        // then
        assertThat(postRepository.findById(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 게시글 없음")
    void deletePost_fail_postNotFound() {
        // given
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> postService.deletePost(999L, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 작성자 아님")
    void deletePost_fail_noPermission() {
        // given
        User writer = User.createUser("writer3", "writer3@example.com", "encodedPwd");
        writer.setUserProfile(new UserProfile(writer, "작성자3", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User another = User.createUser("other", "other@example.com", "encodedPwd");
        another.setUserProfile(new UserProfile(another, "다른사람", null, null, null, 0));
        another.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(another);

        Post post = new Post(writer, "원래 제목", "원래 내용", null);
        postRepository.save(post);

        // when & then
        assertThatThrownBy(() -> postService.deletePost(post.getId(), another.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NO_PERMISSION.getMessage());
    }
}
