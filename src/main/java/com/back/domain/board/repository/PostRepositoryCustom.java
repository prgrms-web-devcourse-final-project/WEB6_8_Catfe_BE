package com.back.domain.board.repository;

import com.back.domain.board.dto.PostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    Page<PostListResponse> searchPosts(String keyword, String searchType, Long categoryId, Pageable pageable);
}