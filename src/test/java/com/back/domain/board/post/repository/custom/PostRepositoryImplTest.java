package com.back.domain.board.post.repository.custom;

import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.entity.*;
import com.back.domain.board.post.enums.CategoryType;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.board.post.repository.PostCategoryRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class PostRepositoryImplTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostCategoryRepository categoryRepository;

    private User user;
    private PostCategory math, science, teen, group2;
    private Post post1, post2, post3;

    @BeforeEach
    void setUp() {
        // 사용자
        user = User.createUser("user1", "user1@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 카테고리
        math = categoryRepository.save(new PostCategory("수학", CategoryType.SUBJECT));
        science = categoryRepository.save(new PostCategory("과학", CategoryType.SUBJECT));
        teen = categoryRepository.save(new PostCategory("10대", CategoryType.DEMOGRAPHIC));
        group2 = categoryRepository.save(new PostCategory("2인", CategoryType.GROUP_SIZE));

        // 게시글
        post1 = new Post(user, "수학 공부 팁", "내용1", null);
        post2 = new Post(user, "과학 토론 모집", "내용2", null);
        post3 = new Post(user, "10대 대상 스터디", "내용3", null);
        postRepository.saveAll(List.of(post1, post2, post3));

        // 카테고리 매핑
        post1.updateCategories(List.of(math, teen));
        post2.updateCategories(List.of(science));
        post3.updateCategories(List.of(teen, group2));
    }

    @Test
    @DisplayName("기본 게시글 목록 조회 (카테고리, 키워드 없이)")
    void searchPosts_basic() {
        // given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<PostListResponse> page = postRepository.searchPosts(null, null, null, pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting("title")
                .containsExactlyInAnyOrder("수학 공부 팁", "과학 토론 모집", "10대 대상 스터디");
    }

    @Test
    @DisplayName("검색어(keyword)와 searchType(title/content/author)에 따른 필터링이 적용된다")
    void searchPosts_withKeyword() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        String keyword = "과학";

        // when
        Page<PostListResponse> page = postRepository.searchPosts(keyword, "title", null, pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getTitle()).contains("과학");
    }

    @Test
    @DisplayName("같은 타입(CategoryType.SUBJECT)은 OR, 다른 타입은 AND로 결합된다")
    void searchPosts_withCategories() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        // SUBJECT 타입 2개 (math, science) + DEMOGRAPHIC 타입 1개 (teen)
        List<Long> categoryIds = List.of(math.getId(), science.getId(), teen.getId());

        // when
        Page<PostListResponse> page = postRepository.searchPosts(null, null, categoryIds, pageable);

        // then
        // SUBJECT는 (math OR science), DEMOGRAPHIC은 (teen) → (math OR science) AND teen
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("수학 공부 팁");
    }

    @Test
    @DisplayName("허용되지 않은 정렬 필드 사용 시 기본 정렬(createdAt DESC) 적용")
    void searchPosts_sortFallback() {
        // given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "unknownField"));

        // when
        Page<PostListResponse> page = postRepository.searchPosts(null, null, null, pageable);

        // then
        // createdAt DESC 기본 정렬 적용
        assertThat(page.getContent().getFirst().getPostId()).isEqualTo(post3.getId());
    }

    @Test
    @DisplayName("게시글이 없는 경우 빈 페이지 반환")
    void searchPosts_empty() {
        // given
        PageRequest pageable = PageRequest.of(0, 5);
        Page<PostListResponse> page = postRepository.searchPosts("없는제목", "title", null, pageable);

        // then
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }
}
