package com.back.domain.board.comment.service;

import com.back.domain.board.comment.dto.CommentLikeResponse;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.entity.CommentLike;
import com.back.domain.board.comment.repository.CommentLikeRepository;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.notification.event.community.CommentLikedEvent;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentLikeService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 댓글 좋아요 서비스
     * 1. User 조회
     * 2. Comment 조회
     * 3. 이미 존재하는 경우 예외 처리
     * 4. CommentLike 저장 및 likeCount 증가
     * 5. 알림 이벤트 발행 (자기 글이 아닐 경우)
     */
    public CommentLikeResponse likeComment(Long commentId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Comment 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 이미 좋아요를 누른 경우 예외
        if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new CustomException(ErrorCode.COMMENT_ALREADY_LIKED);
        }

        // 좋아요 수 증가
        comment.increaseLikeCount();

        // CommentLike 저장 및 응답 반환
        commentLikeRepository.save(new CommentLike(comment, user));

        if (!comment.getUser().getId().equals(userId)) {
            eventPublisher.publishEvent(
                    new CommentLikedEvent(
                            userId,                      // 좋아요 누른 사람
                            comment.getUser().getId(),   // 댓글 작성자
                            comment.getPost().getId(),   // 게시글 ID
                            comment.getId(),
                            comment.getContent()
                    )
            );
        }

        return CommentLikeResponse.from(comment);
    }

    /**
     * 댓글 좋아요 취소 서비스
     * 1. User 조회
     * 2. Comment 조회
     * 3. CommentLike 조회
     * 4. CommentLike 삭제 및 likeCount 감소
     */
    public CommentLikeResponse cancelLikeComment(Long commentId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Comment 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // CommentLike 조회
        CommentLike commentLike = commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_LIKE_NOT_FOUND));

        // 연관관계 제거
        comment.removeLike(commentLike);
        user.removeCommentLike(commentLike);

        // CommentLike 삭제
        commentLikeRepository.delete(commentLike);

        // 좋아요 수 감소
        comment.decreaseLikeCount();

        // 응답 반환
        return CommentLikeResponse.from(comment);
    }
}
