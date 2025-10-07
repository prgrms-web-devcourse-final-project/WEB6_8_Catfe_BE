package com.back.domain.board.post.service;

import com.back.domain.board.post.dto.CategoryResponse;
import com.back.domain.board.post.repository.PostCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCategoryService {
    private final PostCategoryRepository postCategoryRepository;

    /**
     * 카테고리 전체 조회 서비스
     * 1. PostCategory 전체 조회
     * 2. List<CategoryResponse> 반환
     */
    public List<CategoryResponse> getAllCategories() {
        return postCategoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
