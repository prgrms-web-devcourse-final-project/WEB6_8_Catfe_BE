package com.back.domain.board.repository;

import com.back.domain.board.dto.CommentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentRepositoryCustom {
    Page<CommentListResponse> getCommentsByPostId(Long postId, Pageable pageable);
}
