package com.back.domain.board.post.dto;

import com.back.domain.board.common.dto.AuthorResponse;
import com.back.domain.board.post.entity.Post;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 목록 응답 DTO
 */
@Getter
public class PostListResponse {
    private final Long postId;
    private final AuthorResponse author;
    private final String title;
    private final String thumbnailUrl;

    @Setter
    private List<CategoryResponse> categories;

    private final long likeCount;
    private final long bookmarkCount;
    private final long commentCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @QueryProjection
    public PostListResponse(Long postId,
                            AuthorResponse author,
                            String title,
                            String thumbnailUrl,
                            List<CategoryResponse> categories,
                            long likeCount,
                            long bookmarkCount,
                            long commentCount,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this.postId = postId;
        this.author = author;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.categories = categories;
        this.likeCount = likeCount;
        this.bookmarkCount = bookmarkCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PostListResponse from(Post post) {
        return new PostListResponse(
                post.getId(),
                AuthorResponse.from(post.getUser()),
                post.getTitle(),
                post.getThumbnailUrl(),
                post.getCategories().stream()
                        .map(CategoryResponse::from)
                        .toList(),
                post.getLikeCount(),
                post.getBookmarkCount(),
                post.getCommentCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}