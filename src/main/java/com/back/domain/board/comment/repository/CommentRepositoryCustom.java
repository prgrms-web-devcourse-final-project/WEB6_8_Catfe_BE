package com.back.domain.board.comment.repository;

import com.back.domain.board.comment.dto.CommentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentRepositoryCustom {
    Page<CommentListResponse> getCommentsByPostId(Long postId, Pageable pageable);
}
