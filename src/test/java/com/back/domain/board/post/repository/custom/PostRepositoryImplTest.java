package com.back.domain.board.post.repository.custom;

import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.entity.*;
import com.back.domain.board.post.enums.CategoryType;
import com.back.domain.board.post.repository.PostBookmarkRepository;
import com.back.domain.board.post.repository.PostCategoryMappingRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class PostRepositoryImplTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostCategoryRepository categoryRepository;

    private User user;
    private PostCategory math, science, teen, group2;
    private Post post1, post2, post3;
    @Autowired
    private PostCategoryMappingRepository postCategoryMappingRepository;

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
        PostCategoryMapping mapping1 = new PostCategoryMapping(post1, math);
        PostCategoryMapping mapping2 = new PostCategoryMapping(post1, teen);
        PostCategoryMapping mapping3 = new PostCategoryMapping(post2, science);
        PostCategoryMapping mapping4 = new PostCategoryMapping(post3, teen);
        PostCategoryMapping mapping5 = new PostCategoryMapping(post3, group2);
        postCategoryMappingRepository.saveAll(List.of(mapping1, mapping2, mapping3, mapping4, mapping5));
    }

    // ====================== 게시글 다건 검색 테스트 ======================

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

    // ====================== 특정 사용자의 게시글 목록 조회 테스트 ======================

    @Test
    @DisplayName("특정 사용자의 게시글 목록 페이징 조회")
    void findPostsByUserId_basic() {
        // given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<PostListResponse> page = postRepository.findPostsByUserId(user.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(3);

        // 게시글 제목 확인
        assertThat(page.getContent())
                .extracting("title")
                .contains("수학 공부 팁", "과학 토론 모집", "10대 대상 스터디");

        // 작성자 정보가 즉시 조회되었는지 확인
        PostListResponse first = page.getContent().getFirst();
        assertThat(first.getAuthor().nickname()).isEqualTo("작성자");

        // 카테고리 목록이 정상 주입되었는지 (별도 쿼리 1회로 주입되는 구조)
        assertThat(first.getCategories()).isNotEmpty();
        assertThat(first.getCategories())
                .extracting("name")
                .containsAnyOf("수학", "과학", "10대", "2인");
    }

    @Test
    @DisplayName("사용자가 작성한 게시글이 없으면 빈 페이지 반환")
    void findPostsByUserId_empty() {
        // given: 다른 사용자
        User other = User.createUser("other", "other@example.com", "encodedPwd");
        other.setUserProfile(new UserProfile(other, "다른작성자", null, null, null, 0));
        userRepository.save(other);

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Page<PostListResponse> page = postRepository.findPostsByUserId(other.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("정렬 조건(createdAt DESC)이 올바르게 적용")
    void findPostsByUserId_sorting() throws InterruptedException {
        // given
        Thread.sleep(5);
        Post early = new Post(user, "이전 글", "내용", null);
        postRepository.save(early);

        Thread.sleep(5); // createdAt 차이를 확실히 줌
        Post latest = new Post(user, "최근 글", "내용", null);
        postRepository.save(latest);

        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<PostListResponse> page = postRepository.findPostsByUserId(user.getId(), pageable);

        // then
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("최근 글");
        assertThat(page.getContent().get(1).getTitle()).isEqualTo("이전 글");
    }


    @Test
    @DisplayName("카테고리가 없는 게시글도 정상 조회")
    void findPostsByUserId_noCategory() {
        // given
        Post uncategorized = new Post(user, "카테고리 없음 글", "내용", null);
        postRepository.save(uncategorized);

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Page<PostListResponse> page = postRepository.findPostsByUserId(user.getId(), pageable);

        // then
        PostListResponse target = page.getContent().stream()
                .filter(p -> p.getTitle().equals("카테고리 없음 글"))
                .findFirst()
                .orElseThrow();

        assertThat(target.getCategories()).isEmpty();
    }

    // ====================== 특정 사용자의 북마크 게시글 목록 조회 테스트 ======================

    @Test
    @DisplayName("특정 사용자의 북마크 게시글 목록 정상 조회")
    void findBookmarkedPostsByUserId_basic() {
        // given
        PostBookmark b1 = new PostBookmark(post1, user);
        PostBookmark b2 = new PostBookmark(post3, user);
        postBookmarkRepository.saveAll(List.of(b1, b2));

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<PostListResponse> page = postRepository.findBookmarkedPostsByUserId(user.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);

        // 게시글 제목 확인
        assertThat(page.getContent())
                .extracting("title")
                .contains("수학 공부 팁", "10대 대상 스터디");

        // 작성자 정보 확인
        PostListResponse first = page.getContent().getFirst();
        assertThat(first.getAuthor().nickname()).isEqualTo("작성자");

        // 카테고리 목록 주입 검증
        assertThat(first.getCategories()).isNotEmpty();
        assertThat(first.getCategories())
                .extracting("name")
                .containsAnyOf("10대", "2인", "수학");
    }

    @Test
    @DisplayName("사용자가 북마크한 게시글이 없으면 빈 페이지 반환")
    void findBookmarkedPostsByUserId_empty() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Page<PostListResponse> page = postRepository.findBookmarkedPostsByUserId(user.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("정렬 조건(createdAt DESC)이 올바르게 적용")
    void findBookmarkedPostsByUserId_sorting() throws InterruptedException {
        // given
        PostBookmark b1 = new PostBookmark(post1, user);
        postBookmarkRepository.save(b1);

        Thread.sleep(5); // 생성 시각 차이를 확실히 줌
        PostBookmark b2 = new PostBookmark(post2, user);
        postBookmarkRepository.save(b2);

        Thread.sleep(5);
        PostBookmark b3 = new PostBookmark(post3, user);
        postBookmarkRepository.save(b3);

        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<PostListResponse> page = postRepository.findBookmarkedPostsByUserId(user.getId(), pageable);

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("10대 대상 스터디");
        assertThat(page.getContent().get(1).getTitle()).isEqualTo("과학 토론 모집");
    }
}
