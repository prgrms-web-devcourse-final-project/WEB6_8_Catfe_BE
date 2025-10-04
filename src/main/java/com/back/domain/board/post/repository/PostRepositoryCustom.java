package com.back.domain.board.post.repository;

import com.back.domain.board.post.dto.PostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    Page<PostListResponse> searchPosts(String keyword, String searchType, Long categoryId, Pageable pageable);
}