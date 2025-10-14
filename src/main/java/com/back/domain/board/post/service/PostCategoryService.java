package com.back.domain.board.post.service;

import com.back.domain.board.post.dto.CategoryRequest;
import com.back.domain.board.post.dto.CategoryResponse;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCategoryService {
    private final PostCategoryRepository postCategoryRepository;
    private final UserRepository userRepository;

    /**
     * 카테고리 생성 서비스
     *
     * @param request 카테고리 생성 요청 본문
     * @param userId  사용자 ID
     * @return 생성된 카테고리 응답 DTO
     */
    public CategoryResponse createCategory(CategoryRequest request, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이미 존재하는 카테고리인 경우 예외 처리
        if (postCategoryRepository.existsByName(request.name())) {
            throw new CustomException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        // PostCategory 생성
        PostCategory category = new PostCategory(request.name(), request.type());
        PostCategory saved = postCategoryRepository.save(category);

        return CategoryResponse.from(saved);
    }

    /**
     * 카테고리 전체 조회 서비스
     *
     * @return 카테고리 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return postCategoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
