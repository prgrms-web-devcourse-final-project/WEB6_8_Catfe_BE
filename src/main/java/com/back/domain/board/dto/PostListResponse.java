package com.back.domain.board.dto;

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
    private final long likeCount;
    private final long bookmarkCount;
    private final long commentCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Setter
    private List<CategoryResponse> categories;

    @QueryProjection
    public PostListResponse(Long postId,
                            AuthorResponse author,
                            String title,
                            List<CategoryResponse> categories,
                            long likeCount,
                            long bookmarkCount,
                            long commentCount,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this.postId = postId;
        this.author = author;
        this.title = title;
        this.categories = categories;
        this.likeCount = likeCount;
        this.bookmarkCount = bookmarkCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 작성자 응답 DTO
     */
    @Getter
    public static class AuthorResponse {
        private final Long id;
        private final String nickname;

        @QueryProjection
        public AuthorResponse(Long userId, String nickname) {
            this.id = userId;
            this.nickname = nickname;
        }
    }

    /**
     * 카테고리 응답 DTO
     */
    @Getter
    public static class CategoryResponse {
        private final Long id;
        private final String name;

        @QueryProjection
        public CategoryResponse(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}