package com.back.domain.board.post.service;

import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.entity.PostCategoryMapping;
import com.back.domain.board.post.repository.PostCategoryMappingRepository;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCategoryMappingService {
    private final PostCategoryMappingRepository postCategoryMappingRepository;
    private final PostCategoryRepository postCategoryRepository;

    /**
     * 게시글 생성 시 카테고리 매핑 등록
     * - 카테고리 ID 유효성 검증 후 매핑 엔티티 생성
     *
     * @param post        게시글 엔티티
     * @param categoryIds 카테고리 ID 리스트
     */
    public void createMappings(Post post, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return;

        // 카테고리 유효성 검증
        List<PostCategory> categories = postCategoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 매핑 생성
        categories.forEach(category -> {
            PostCategoryMapping mapping = new PostCategoryMapping(post, category);
            postCategoryMappingRepository.save(mapping);
        });
    }

    /**
     * 게시글의 카테고리 매핑을 갱신
     * - 기존 매핑 제거 및 신규 매핑 추가 처리
     *
     * @param post        게시글 엔티티
     * @param categoryIds 카테고리 ID 리스트
     */
    public void updateMappings(Post post, List<Long> categoryIds) {
        // null 또는 빈 리스트면 → 모든 매핑 제거
        if (categoryIds == null || categoryIds.isEmpty()) {
            deleteMappings(post);
            return;
        }

        List<PostCategory> newCategories = postCategoryRepository.findAllById(categoryIds);
        if (newCategories.size() != categoryIds.size()) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        List<PostCategoryMapping> currentMappings = post.getPostCategoryMappings();
        List<PostCategory> currentCategories = post.getCategories();

        // 제거 대상
        List<PostCategoryMapping> toRemove = currentMappings.stream()
                .filter(mapping -> !newCategories.contains(mapping.getCategory()))
                .toList();

        // 추가 대상
        List<PostCategory> toAdd = newCategories.stream()
                .filter(category -> !currentCategories.contains(category))
                .toList();

        // 제거 수행
        toRemove.forEach(mapping -> {
            mapping.remove();
            postCategoryMappingRepository.delete(mapping);
        });

        // 추가 수행
        toAdd.forEach(category -> {
            PostCategoryMapping mapping = new PostCategoryMapping(post, category);
            postCategoryMappingRepository.save(mapping);
        });
    }

    /**
     * 게시글의 모든 카테고리 매핑을 삭제
     *
     * @param post 게시글 엔티티
     */
    public void deleteMappings(Post post) {
        List<PostCategoryMapping> existingMappings = post.getPostCategoryMappings();
        existingMappings.forEach(mapping -> {
            mapping.remove();
            postCategoryMappingRepository.delete(mapping);
        });
    }
}
