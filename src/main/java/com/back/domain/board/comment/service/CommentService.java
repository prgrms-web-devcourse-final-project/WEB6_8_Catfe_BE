package com.back.domain.board.comment.service;

import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.dto.CommentRequest;
import com.back.domain.board.comment.dto.CommentResponse;
import com.back.domain.board.comment.dto.ReplyResponse;
import com.back.domain.board.comment.repository.CommentLikeRepository;
import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.notification.event.community.CommentCreatedEvent;
import com.back.domain.notification.event.community.ReplyCreatedEvent;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 댓글 생성 서비스
     *
     * @param postId  대상 게시글 ID
     * @param request 댓글 작성 요청 본문
     * @param userId  사용자 ID
     * @return 생성된 댓글 응답 DTO
     */
    public CommentResponse createComment(Long postId, CommentRequest request, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // Comment 생성 및 댓글 수 증가 처리
        post.increaseCommentCount();
        Comment comment = Comment.createRoot(post, user, request.content());
        commentRepository.save(comment);

        // 댓글 작성 이벤트 발행
        eventPublisher.publishEvent(
                new CommentCreatedEvent(
                        userId,                  // 댓글 작성자
                        post.getUser().getId(),  // 게시글 작성자
                        postId,
                        comment.getId(),
                        request.content()
                )
        );

        return CommentResponse.from(comment);
    }

    /**
     * 댓글 목록 조회 서비스
     *
     * @param postId   게시글 ID
     * @param userId   사용자 ID (선택)
     * @param pageable 페이징 정보
     * @return 댓글 목록 페이지 응답 DTO
     */
    @Transactional(readOnly = true)
    public PageResponse<CommentListResponse> getComments(Long postId, Pageable pageable, Long userId) {
        // Post 검증
        if (!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        // 댓글 목록 페이지 조회 (대댓글 포함)
        Page<CommentListResponse> comments = commentRepository.findCommentsByPostId(postId, pageable);

        // 로그인 사용자 추가 데이터 설정 (좋아요 여부)
        if (userId != null && !comments.isEmpty()) {
            applyLikedByUser(comments.getContent(), userId);
        }

        return PageResponse.from(comments);
    }

    /**
     * 댓글 좋아요 여부(likedByMe) 설정
     */
    private void applyLikedByUser(List<CommentListResponse> comments, Long userId) {
        // 모든 댓글 ID 수집
        List<Long> commentIds = comments.stream()
                .flatMap(parent -> {
                    Stream<Long> childIds = Optional.ofNullable(parent.getChildren())
                            .orElse(List.of())
                            .stream()
                            .map(CommentListResponse::getCommentId);
                    return Stream.concat(Stream.of(parent.getCommentId()), childIds);
                })
                .toList();

        // 사용자가 좋아요한 댓글 ID 조회 (단일 쿼리)
        Set<Long> likedSet = new HashSet<>(commentLikeRepository.findLikedCommentIdsIn(userId, commentIds));

        // likedByMe 플래그 설정
        comments.forEach(parent -> {
            parent.setLikedByMe(likedSet.contains(parent.getCommentId()));
            Optional.ofNullable(parent.getChildren())
                    .ifPresent(children -> children.forEach(child ->
                            child.setLikedByMe(likedSet.contains(child.getCommentId()))));
        });
    }

    /**
     * 댓글 수정 서비스
     *
     * @param postId    게시글 ID
     * @param commentId 댓글 ID
     * @param request   댓글 수정 요청 본문
     * @param userId    사용자 ID
     * @return 수정된 댓글 응답 DTO
     */
    public CommentResponse updateComment(Long postId, Long commentId, CommentRequest request, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // Comment 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 작성자 검증
        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_NO_PERMISSION);
        }

        // Comment 업데이트
        comment.update(request.content());

        return CommentResponse.from(comment);
    }

    /**
     * 댓글 삭제 서비스
     *
     * @param postId    게시글 ID
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     */
    public void deleteComment(Long postId, Long commentId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // Comment 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 작성자 검증
        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.COMMENT_NO_PERMISSION);
        }

        // Comment 삭제 및 댓글 수 감소 처리
        comment.remove();
        commentRepository.delete(comment);
        post.decreaseCommentCount();
    }

    /**
     * 대댓글 생성 서비스
     *
     * @param postId          게시글 ID
     * @param parentCommentId 부모 댓글 ID
     * @param request         대댓글 작성 요청 본문
     * @param userId          사용자 ID
     * @return 생성된 대댓글 응답 DTO
     */
    public ReplyResponse createReply(Long postId, Long parentCommentId, CommentRequest request, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 부모 Comment 조회
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 부모의 게시글 일치 검증
        if (!parent.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.COMMENT_PARENT_MISMATCH);
        }

        // depth 검증: 부모가 이미 대댓글이면 예외
        if (parent.getParent() != null) {
            throw new CustomException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
        }

        // 자식 Comment 생성
        Comment reply = new Comment(post, user, request.content(), parent);
        commentRepository.save(reply);

        // 대댓글 작성 이벤트 발행
        eventPublisher.publishEvent(
                new ReplyCreatedEvent(
                        userId,                      // 대댓글 작성자
                        parent.getUser().getId(),    // 댓글 작성자
                        postId,
                        parentCommentId,
                        reply.getId(),
                        request.content()
                )
        );

        return ReplyResponse.from(reply);
    }
}
