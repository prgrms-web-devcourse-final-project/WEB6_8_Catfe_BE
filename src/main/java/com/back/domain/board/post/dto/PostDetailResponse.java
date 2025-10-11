package com.back.domain.board.post.dto;

import com.back.domain.board.common.dto.AuthorResponse;
import com.back.domain.board.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 상세 응답 DTO
 *
 * @param postId        게시글 ID
 * @param author        작성자 정보
 * @param title         게시글 제목
 * @param content       게시글 내용
 * @param thumbnailUrl  썸네일 URL
 * @param categories    게시글 카테고리 목록
 * @param likeCount     좋아요 수
 * @param bookmarkCount 북마크 수
 * @param commentCount  댓글 수
 * @param createdAt     게시글 생성 일시
 * @param updatedAt     게시글 수정 일시
 */
public record PostDetailResponse(
        Long postId,
        AuthorResponse author,
        String title,
        String content,
        String thumbnailUrl,
        List<CategoryResponse> categories,
        long likeCount,
        long bookmarkCount,
        long commentCount,
        Boolean likedByMe,
        Boolean bookmarkedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse from(Post post) {
        return from(post, false, false);
    }

    public static PostDetailResponse from(Post post, boolean likedByMe, boolean bookmarkedByMe) {
        return new PostDetailResponse(
                post.getId(),
                AuthorResponse.from(post.getUser()),
                post.getTitle(),
                post.getContent(),
                post.getThumbnailUrl(),
                post.getCategories().stream()
                        .map(CategoryResponse::from)
                        .toList(),
                post.getPostLikes().size(),
                post.getPostBookmarks().size(),
                post.getComments().size(),
                likedByMe,
                bookmarkedByMe,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
