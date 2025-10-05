package com.back.domain.board.comment.service;

import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.dto.CommentRequest;
import com.back.domain.board.comment.dto.CommentResponse;
import com.back.domain.board.comment.dto.ReplyResponse;
import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    /**
     * 댓글 생성 서비스
     * 1. User 조회
     * 2. Post 조회
     * 3. Comment 생성
     * 4. Comment 저장 및 CommentResponse 반환
     */
    public CommentResponse createComment(Long postId, CommentRequest request, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // Comment 생성
        Comment comment = new Comment(post, user, request.content());

        // Comment 저장 및 응답 반환
        commentRepository.save(comment);
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
        return ReplyResponse.from(reply);
    }
}
