package com.back.domain.board.post.service;

import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.dto.PostDetailResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.dto.PostRequest;
import com.back.domain.board.post.dto.PostResponse;
import com.back.domain.board.post.entity.PostCategoryMapping;
import com.back.domain.board.post.repository.PostBookmarkRepository;
import com.back.domain.board.post.repository.PostCategoryRepository;
import com.back.domain.board.post.repository.PostLikeRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.entity.AttachmentMapping;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.AttachmentMappingRepository;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
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
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final UserRepository userRepository;
    private final PostCategoryRepository postCategoryRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final AttachmentMappingRepository attachmentMappingRepository;

    /**
     * 게시글 생성 서비스
     *
     * @param request 게시글 작성 요청 본문
     * @param userId  사용자 ID
     * @return 생성된 게시글 응답 DTO
     */
    public PostResponse createPost(PostRequest request, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 생성
        Post post = new Post(user, request.title(), request.content(), request.thumbnailUrl());
        postRepository.save(post);

        // Category 매핑
        if (request.categoryIds() != null) {
            List<PostCategory> categories = validateAndFindCategories(request.categoryIds());
            categories.forEach(category -> new PostCategoryMapping(post, category));
        }

        // AttachmentMapping 매핑
        List<FileAttachment> attachments = List.of();
        if (request.imageIds() != null && !request.imageIds().isEmpty()) {
            attachments = validateAndFindAttachments(request.imageIds());
            for (FileAttachment attachment : attachments) {
                attachmentMappingRepository.save(new AttachmentMapping(attachment, EntityType.POST, post.getId()));
            }
        }

        return PostResponse.from(post, attachments);
    }

    /**
     * 게시글 다건 조회 서비스
     *
     * @param keyword     검색어
     * @param searchType  검색 타입
     * @param categoryIds 카테고리 ID 목록
     * @param pageable    페이징 정보
     * @return 게시글 목록 페이지 응답 DTO
     */
    @Transactional(readOnly = true)
    public PageResponse<PostListResponse> getPosts(String keyword, String searchType, List<Long> categoryIds, Pageable pageable) {
        Page<PostListResponse> posts = postRepository.searchPosts(keyword, searchType, categoryIds, pageable);
        return PageResponse.from(posts);
    }

    /**
     * 게시글 단건 조회 서비스
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 게시글 상세 응답 DTO
     */
    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long postId, Long userId) {
        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 첨부파일 조회
        List<FileAttachment> attachments = attachmentMappingRepository
                .findAllByEntityTypeAndEntityId(EntityType.POST, post.getId())
                .stream()
                .map(AttachmentMapping::getFileAttachment)
                .toList();

        // 로그인 사용자 추가 데이터 설정 (좋아요, 북마크 여부)
        if (userId != null) {
            boolean likedByMe = postLikeRepository.existsByUserIdAndPostId(userId, post.getId());
            boolean bookmarkedByMe = postBookmarkRepository.existsByUserIdAndPostId(userId, post.getId());
            return PostDetailResponse.from(post, attachments, likedByMe, bookmarkedByMe);
        }

        // 비로그인 사용자는 기본 응답 반환
        return PostDetailResponse.from(post, attachments);
    }

    /**
     * 게시글 수정 서비스
     *
     * @param postId  게시글 ID
     * @param request 게시글 수정 요청 본문
     * @param userId  사용자 ID
     * @return 수정된 게시글 응답 DTO
     */
    public PostResponse updatePost(Long postId, PostRequest request, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

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
        List<PostCategory> categories = validateAndFindCategories(request.categoryIds());
        post.updateCategories(categories);

        // AttachmentMapping 매핑
        attachmentMappingRepository.deleteAllByEntityTypeAndEntityId(EntityType.POST, post.getId());
        List<FileAttachment> attachments = List.of();
        if (request.imageIds() != null && !request.imageIds().isEmpty()) {
            attachments = validateAndFindAttachments(request.imageIds());
            attachments.forEach(attachment ->
                    attachmentMappingRepository.save(new AttachmentMapping(attachment, EntityType.POST, post.getId()))
            );
        }

        return PostResponse.from(post, attachments);
    }

    /**
     * 게시글 삭제 서비스
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     */
    public void deletePost(Long postId, Long userId) {
        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 작성자 검증
        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.POST_NO_PERMISSION);
        }

        // AttachmentMapping 매핑 제거
        attachmentMappingRepository.deleteAllByEntityTypeAndEntityId(EntityType.POST, post.getId());

        // Post 삭제
        post.remove();
        postRepository.delete(post);
    }

    /**
     * 카테고리 ID 유효성 검증 및 조회
     */
    private List<PostCategory> validateAndFindCategories(List<Long> categoryIds) {
        return categoryIds.stream()
                .map(id -> postCategoryRepository.findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND)))
                .toList();
    }

    /**
     * 첨부 파일 ID 유효성 검증 및 조회
     */
    private List<FileAttachment> validateAndFindAttachments(List<Long> imageIds) {
        return imageIds.stream()
                .map(id -> fileAttachmentRepository.findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND)))
                .toList();
    }
}