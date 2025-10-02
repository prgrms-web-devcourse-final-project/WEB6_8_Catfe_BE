package com.back.domain.board.service;

import com.back.domain.board.dto.PostRequest;
import com.back.domain.board.dto.PostResponse;
import com.back.domain.board.entity.Post;
import com.back.domain.board.entity.PostCategory;
import com.back.domain.board.repository.PostCategoryRepository;
import com.back.domain.board.repository.PostRepository;
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
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostCategoryRepository postCategoryRepository;

    /**
     * 게시글 생성 서비스
     * 1. User 조회
     * 2. Post 생성
     * 3. Category 매핑
     * 4. Post 저장 및 PostResponse 반환
     */
    public PostResponse createPost(PostRequest request, Long userId) {

        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 생성
        Post post = new Post(user, request.title(), request.content());

        // Category 매핑
        if (request.categoryIds() != null) {
            List<PostCategory> categories = postCategoryRepository.findAllById(request.categoryIds());
            if (categories.size() != request.categoryIds().size()) {
                throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            post.updateCategories(categories);
        }

        // Post 저장 및 응답 반환
        Post saved = postRepository.save(post);
        return PostResponse.from(saved);
    }
}