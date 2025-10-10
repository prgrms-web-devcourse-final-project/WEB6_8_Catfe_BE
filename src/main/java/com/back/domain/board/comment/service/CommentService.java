package com.back.domain.board.comment.service;

import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.dto.CommentRequest;
import com.back.domain.board.comment.dto.CommentResponse;
import com.back.domain.board.comment.dto.ReplyResponse;
import com.back.domain.board.comment.entity.CommentLike;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;

    // TODO: 연관관계 고려, 메서드 명, 중복 코드 제거, 주석 통일
    // TODO: comment 끝나면 post도 해야 함.. entity > DTO > Repo > Service > Controller > Docs 순으로..
    /**
     * 댓글 생성 서비스
     * 1. User 조회
     * 2. Post 조회
     * 3. Comment 생성 및 저장
     * 4. 댓글 작성 이벤트 발행
     * 5. CommentResponse 반환
     */
    public CommentResponse createComment(Long postId, CommentRequest request, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // CommentCount 증가
        post.increaseCommentCount();

        // Comment 생성 및 저장
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
     * 댓글 다건 조회 서비스
     * 1. Post 조회
     * 2. 해당 Post의 댓글 전체 조회 (대댓글 포함, 페이징)
     * 3. PageResponse 반환
     */
    @Transactional(readOnly = true)
    public PageResponse<CommentListResponse> getComments(Long postId, Pageable pageable) {
        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 댓글 목록 조회
        Page<CommentListResponse> comments = commentRepository.getCommentsByPostId(postId, pageable);

        return PageResponse.from(comments);
    }

    // TODO: 추후 메서드 통합 및 리팩토링
    @Transactional(readOnly = true)
    public PageResponse<CommentListResponse> getComments(Long postId, Long userId, Pageable pageable) {
        // 기본 댓글 목록
        PageResponse<CommentListResponse> response = getComments(postId, pageable);

        // 로그인 사용자용 로직
        if (userId != null) {
            // 댓글 ID 수집
            List<Long> commentIds = response.items().stream()
                    .map(CommentListResponse::getCommentId)
                    .toList();

            if (commentIds.isEmpty()) return response;

            // QueryDSL 기반 좋아요 ID 조회 (단일 쿼리)
            List<Long> likedIds = commentLikeRepository.findLikedCommentIdsIn(userId, commentIds);
            Set<Long> likedSet = new HashSet<>(likedIds);

            // likedByMe 세팅
            response.items().forEach(c -> c.setLikedByMe(likedSet.contains(c.getCommentId())));

            // 자식 댓글에도 동일 적용
            response.items().forEach(parent -> {
                if (parent.getChildren() != null) {
                    parent.getChildren().forEach(child ->
                            child.setLikedByMe(likedSet.contains(child.getCommentId()))
                    );
                }
            });
        }

        return response;
    }

    /**
     * 댓글 수정 서비스
     * 1. Post 조회
     * 2. Comment 조회
     * 3. 작성자 검증
     * 4. Comment 업데이트 (내용)
     * 5. CommentResponse 반환
     */
    public CommentResponse updateComment(Long postId, Long commentId, CommentRequest request, Long userId) {
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
        
        // 응답 반환
        return CommentResponse.from(comment);
    }

    /**
     * 댓글 삭제 서비스
     * 1. Post 조회
     * 2. Comment 조회
     * 3. 작성자 검증
     * 4. Comment 삭제
     */
    public void deleteComment(Long postId, Long commentId, Long userId) {
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

        commentRepository.delete(comment);
    }

    /**
     * 대댓글 생성 서비스
     * 1. User 조회
     * 2. Post 조회
     * 3. 부모 Comment 조회
     * 4. 부모 및 depth 검증
     * 5. 자식 Comment 생성
     * 6. Comment 저장 및 ReplyResponse 반환
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

        // 저장 및 응답 반환
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
