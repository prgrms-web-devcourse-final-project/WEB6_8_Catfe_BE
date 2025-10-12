package com.back.domain.board.comment.repository.custom;

import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.entity.Comment;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class CommentRepositoryImplTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Post post;
    private Comment parent1, parent2, parent3;
    private Comment child11, child12, child21;

    @BeforeEach
    void setUp() {
        // 사용자
        user = User.createUser("user1", "user1@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // 게시글
        post = new Post(user, "게시글 제목", "내용", null);
        postRepository.save(post);

        // 부모 댓글 3개
        parent1 = Comment.createRoot(post, user, "부모1");
        parent2 = Comment.createRoot(post, user, "부모2");
        parent3 = Comment.createRoot(post, user, "부모3");
        commentRepository.saveAll(List.of(parent1, parent2, parent3));

        // 자식 댓글
        child11 = Comment.createChild(post, user, "부모1의 자식1", parent1);
        child12 = Comment.createChild(post, user, "부모1의 자식2", parent1);
        child21 = Comment.createChild(post, user, "부모2의 자식1", parent2);
        commentRepository.saveAll(List.of(child11, child12, child21));
    }

    @Test
    @DisplayName("게시글의 부모 댓글 목록과 자식 댓글이 함께 조회된다 (총 쿼리 3회 예상)")
    void getCommentsByPostId_success() {
        // given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<CommentListResponse> page = commentRepository.getCommentsByPostId(post.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(3L); // 부모 3개
        assertThat(page.getContent()).hasSize(3);

        CommentListResponse p1 = page.getContent().stream()
                .filter(c -> c.getCommentId().equals(parent1.getId()))
                .findFirst()
                .orElseThrow();

        CommentListResponse p2 = page.getContent().stream()
                .filter(c -> c.getCommentId().equals(parent2.getId()))
                .findFirst()
                .orElseThrow();

        // 부모-자식 매핑 검증
        assertThat(p1.getChildren()).extracting("commentId")
                .containsExactlyInAnyOrder(child11.getId(), child12.getId());
        assertThat(p2.getChildren()).extracting("commentId")
                .containsExactly(child21.getId());

        // 부모3은 자식 없음
        CommentListResponse p3 = page.getContent().stream()
                .filter(c -> c.getCommentId().equals(parent3.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(p3.getChildren()).isEmpty();
    }

    @Test
    @DisplayName("댓글이 없는 게시글은 빈 페이지 반환")
    void getCommentsByPostId_empty() {
        // given
        Post newPost = new Post(user, "새 게시글", "내용", null);
        postRepository.save(newPost);
        PageRequest pageable = PageRequest.of(0, 5);

        // when
        Page<CommentListResponse> page = commentRepository.getCommentsByPostId(newPost.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("정렬 조건이 허용되지 않으면 기본 정렬(createdAt DESC)로 동작한다")
    void getCommentsByPostId_sortFallback() {
        // given: 허용되지 않은 정렬 필드 (likeCount만 허용됨)
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "unknownField"));

        // when
        Page<CommentListResponse> page = commentRepository.getCommentsByPostId(post.getId(), pageable);

        // then
        // createdAt DESC 기본 정렬이 적용되어, 마지막에 생성된 parent3이 먼저 나와야 함
        assertThat(page.getContent().get(0).getCommentId()).isEqualTo(parent3.getId());
    }
}
