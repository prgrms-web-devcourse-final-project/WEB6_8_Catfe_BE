package com.back.domain.board.comment.service;

import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.dto.CommentRequest;
import com.back.domain.board.comment.dto.CommentResponse;
import com.back.domain.board.comment.dto.ReplyResponse;
import com.back.domain.board.comment.service.CommentService;
import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.notification.service.NotificationService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @MockitoBean
    private NotificationService notificationService;

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

    // ====================== 댓글 조회 테스트 ======================

    @Test
    @DisplayName("댓글 목록 조회 성공 - 부모 + 자식 포함")
    void getComments_success() {
        // given: 유저 + 게시글
        User user = User.createUser("writer", "writer@example.com", "pwd");
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        // 부모 댓글
        Comment parent = new Comment(post, user, "부모 댓글", null);
        commentRepository.save(parent);

        // 자식 댓글
        Comment child = new Comment(post, user, "자식 댓글", parent);
        commentRepository.save(child);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

        // when
        PageResponse<CommentListResponse> response = commentService.getComments(post.getId(), pageable);

        // then
        assertThat(response.items()).hasSize(1); // 부모만 페이징 결과
        CommentListResponse parentRes = response.items().getFirst();
        assertThat(parentRes.getContent()).isEqualTo("부모 댓글");
        assertThat(parentRes.getChildren()).hasSize(1);
        assertThat(parentRes.getChildren().getFirst().getContent()).isEqualTo("자식 댓글");
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 게시글 없음")
    void getComments_fail_postNotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() ->
                commentService.getComments(999L, pageable)
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    // ====================== 댓글 수정 테스트 ======================

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_success() {
        // given: 유저 + 게시글 + 댓글
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "원래 댓글");
        commentRepository.save(comment);

        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        // when
        CommentResponse response = commentService.updateComment(post.getId(), comment.getId(), updateRequest, user.getId());

        // then
        assertThat(response.content()).isEqualTo("수정된 댓글");
        assertThat(response.commentId()).isEqualTo(comment.getId());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 존재하지 않는 게시글")
    void updateComment_fail_postNotFound() {
        // given: 유저 + 댓글
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "댓글");
        commentRepository.save(comment);

        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        // when & then
        assertThatThrownBy(() ->
                commentService.updateComment(999L, comment.getId(), updateRequest, user.getId())
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 존재하지 않는 댓글")
    void updateComment_fail_commentNotFound() {
        // given: 유저 + 게시글
        User user = User.createUser("writer3", "writer3@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자3", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        // when & then
        assertThatThrownBy(() ->
                commentService.updateComment(post.getId(), 999L, updateRequest, user.getId())
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 작성자가 아님")
    void updateComment_fail_noPermission() {
        // given: 유저 2명 + 게시글 + 댓글
        User writer = User.createUser("writer", "writer@example.com", "encodedPwd");
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User other = User.createUser("other", "other@example.com", "encodedPwd");
        other.setUserProfile(new UserProfile(other, "다른사람", null, null, null, 0));
        other.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(other);

        Post post = new Post(writer, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, writer, "원래 댓글");
        commentRepository.save(comment);

        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        // when & then
        assertThatThrownBy(() ->
                commentService.updateComment(post.getId(), comment.getId(), updateRequest, other.getId())
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NO_PERMISSION.getMessage());
    }

    // ====================== 댓글 삭제 테스트 ======================

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_success() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "삭제할 댓글");
        commentRepository.save(comment);

        // when
        commentService.deleteComment(post.getId(), comment.getId(), user.getId());

        // then
        assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 게시글")
    void deleteComment_fail_postNotFound() {
        // given
        User user = User.createUser("writer2", "writer2@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자2", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, user, "댓글");
        commentRepository.save(comment);

        // when & then
        assertThatThrownBy(() ->
                commentService.deleteComment(999L, comment.getId(), user.getId())
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    void deleteComment_fail_commentNotFound() {
        // given
        User user = User.createUser("writer3", "writer3@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자3", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        // when & then
        assertThatThrownBy(() ->
                commentService.deleteComment(post.getId(), 999L, user.getId())
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자가 아님")
    void deleteComment_fail_noPermission() {
        // given: 작성자 + 다른 사용자
        User writer = User.createUser("writer", "writer@example.com", "encodedPwd");
        writer.setUserProfile(new UserProfile(writer, "작성자", null, null, null, 0));
        writer.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(writer);

        User other = User.createUser("other", "other@example.com", "encodedPwd");
        other.setUserProfile(new UserProfile(other, "다른사람", null, null, null, 0));
        other.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(other);

        Post post = new Post(writer, "제목", "내용");
        postRepository.save(post);

        Comment comment = new Comment(post, writer, "원래 댓글");
        commentRepository.save(comment);

        // when & then
        assertThatThrownBy(() ->
                commentService.deleteComment(post.getId(), comment.getId(), other.getId())
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NO_PERMISSION.getMessage());
    }

    // ====================== 대댓글 테스트 ======================

    @Test
    @DisplayName("대댓글 생성 성공")
    void createReply_success() {
        // given: 유저 + 게시글 + 부모 댓글 저장
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        commentRepository.save(parent);

        CommentRequest request = new CommentRequest("대댓글 내용");

        // when
        ReplyResponse response = commentService.createReply(post.getId(), parent.getId(), request, user.getId());

        // then
        assertThat(response.content()).isEqualTo("대댓글 내용");
        assertThat(response.author().nickname()).isEqualTo("작성자");
        assertThat(response.parentId()).isEqualTo(parent.getId());
        assertThat(response.postId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 부모 댓글이 다른 게시글에 속함")
    void createReply_fail_parentMismatch() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post1 = new Post(user, "게시글1", "내용1");
        Post post2 = new Post(user, "게시글2", "내용2");
        postRepository.saveAll(List.of(post1, post2));

        Comment parent = new Comment(post1, user, "부모 댓글", null);
        commentRepository.save(parent);

        CommentRequest request = new CommentRequest("대댓글 내용");

        // when & then
        assertThatThrownBy(() -> commentService.createReply(post2.getId(), parent.getId(), request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_PARENT_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 부모 댓글이 이미 대댓글(depth 초과)")
    void createReply_fail_depthExceeded() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        // 부모 댓글 + 그 부모의 대댓글까지 생성
        Comment parent = new Comment(post, user, "부모 댓글", null);
        Comment child = new Comment(post, user, "대댓글1", parent);
        commentRepository.saveAll(List.of(parent, child));

        CommentRequest request = new CommentRequest("대댓글2 내용");

        // when & then
        assertThatThrownBy(() -> commentService.createReply(post.getId(), child.getId(), request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_DEPTH_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("대댓글 생성 실패 - 존재하지 않는 부모 댓글")
    void createReply_fail_commentNotFound() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        CommentRequest request = new CommentRequest("대댓글 내용");

        // when & then
        assertThatThrownBy(() -> commentService.createReply(post.getId(), 999L, request, user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("대댓글 수정 성공")
    void updateReply_success() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        Comment reply = new Comment(post, user, "대댓글", parent);
        commentRepository.saveAll(List.of(parent, reply));

        CommentRequest updateRequest = new CommentRequest("수정된 대댓글 내용");

        // when
        CommentResponse updated = commentService.updateComment(post.getId(), reply.getId(), updateRequest, user.getId());

        // then
        assertThat(updated.content()).isEqualTo("수정된 대댓글 내용");
        assertThat(updated.commentId()).isEqualTo(reply.getId());
        assertThat(updated.author().nickname()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("대댓글 삭제 성공")
    void deleteReply_success() {
        // given
        User user = User.createUser("writer", "writer@example.com", "encodedPwd");
        user.setUserProfile(new UserProfile(user, "작성자", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "제목", "내용");
        postRepository.save(post);

        Comment parent = new Comment(post, user, "부모 댓글", null);
        Comment reply = new Comment(post, user, "삭제할 대댓글", parent);
        commentRepository.saveAll(List.of(parent, reply));

        // when
        commentService.deleteComment(post.getId(), reply.getId(), user.getId());

        // then
        boolean exists = commentRepository.findById(reply.getId()).isPresent();
        assertThat(exists).isFalse();
    }
}