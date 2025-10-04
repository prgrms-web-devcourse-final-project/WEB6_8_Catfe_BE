package com.back.domain.board.post.service;

import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.dto.PostDetailResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.dto.PostRequest;
import com.back.domain.board.post.dto.PostResponse;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 게시글 다건 조회 서비스
     * 1. Post 검색 (키워드, 검색타입, 카테고리, 페이징)
     * 2. PageResponse 반환
     */
    @Transactional(readOnly = true)
    public PageResponse<PostListResponse> getPosts(String keyword, String searchType, Long categoryId, Pageable pageable) {
        Page<PostListResponse> posts = postRepository.searchPosts(keyword, searchType, categoryId, pageable);
        return PageResponse.from(posts);
    }

    /**
     * 게시글 단건 조회 서비스
     * 1. Post 조회
     * 2. PostResponse 반환
     */
    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long postId) {
        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 응답 반환
        return PostDetailResponse.from(post);
    }

    /**
     * 게시글 수정 서비스
     * 1. Post 조회
     * 2. 작성자 검증
     * 3. Post 업데이트 (제목, 내용, 카테고리)
     * 4. PostResponse 반환
     */
    public PostResponse updatePost(Long postId, PostRequest request, Long userId) {
        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 작성자 검증
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.POST_NO_PERMISSION);
        }

        // Post 업데이트
        post.update(request.title(), request.content());

        // Category 매핑 업데이트
        List<PostCategory> categories = postCategoryRepository.findAllById(request.categoryIds());
        if (categories.size() != request.categoryIds().size()) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        post.updateCategories(categories);

        // 응답 반환
        return PostResponse.from(post);
    }

    /**
     * 게시글 삭제 서비스
     * 1. Post 조회
     * 2. 작성자 검증
     * 3. Post 삭제
     */
    public void deletePost(Long postId, Long userId) {
        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 작성자 검증
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.POST_NO_PERMISSION);
        }

        // Post 삭제
        postRepository.delete(post);
    }
}