package com.back.domain.board.comment.repository.custom;

import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.dto.MyCommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentRepositoryCustom {
    Page<CommentListResponse> findCommentsByPostId(Long postId, Pageable pageable);
    Page<MyCommentResponse> findCommentsByUserId(Long postId, Pageable pageable);
}
