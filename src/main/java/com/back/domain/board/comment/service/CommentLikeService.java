package com.back.domain.board.comment.service;

import com.back.domain.board.comment.dto.CommentLikeResponse;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.entity.CommentLike;
import com.back.domain.board.comment.repository.CommentLikeRepository;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.notification.event.community.CommentLikedEvent;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
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
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return 댓글 좋아요 응답 DTO
     */
    public CommentLikeResponse likeComment(Long commentId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Comment 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 이미 좋아요한 경우 예외
        if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new CustomException(ErrorCode.COMMENT_ALREADY_LIKED);
        }

        // CommentLike 생성 및 좋아요 수 증가 처리
        comment.increaseLikeCount();
        commentLikeRepository.save(new CommentLike(comment, user));

        // 댓글 좋아요 이벤트 발행 (자기 댓글이 아닐 때만)
        if (!comment.getUser().getId().equals(userId)) {
            eventPublisher.publishEvent(
                    new CommentLikedEvent(
                            userId,                      // 좋아요한 사용자
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
     *
     * @param commentId 댓글 ID
     * @param userId    사용자 ID
     * @return 댓글 좋아요 응답 DTO
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

        // CommentLike 삭제 및 좋아요 수 감소 처리
        commentLike.remove();
        commentLikeRepository.delete(commentLike);
        comment.decreaseLikeCount();

        return CommentLikeResponse.from(comment);
    }
}
