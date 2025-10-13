package com.back.domain.board.post.repository.custom;

import com.back.domain.board.post.dto.PostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostRepositoryCustom {
    Page<PostListResponse> searchPosts(String keyword, String searchType, List<Long> categoryIds, Pageable pageable);
    Page<PostListResponse> findPostsByUserId(Long userId, Pageable pageable);
}