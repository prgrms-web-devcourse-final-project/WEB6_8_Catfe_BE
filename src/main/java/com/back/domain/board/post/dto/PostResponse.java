package com.back.domain.board.post.dto;

import com.back.domain.board.common.dto.AuthorResponse;
import com.back.domain.board.post.entity.Post;
import com.back.domain.file.entity.FileAttachment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 응답 DTO
 *
 * @param postId     게시글 ID
 * @param author     작성자 정보
 * @param title      게시글 제목
 * @param content    게시글 내용
 * @param thumbnailUrl  썸네일 URL
 * @param categories   게시글 카테고리 목록
 * @param images         첨부된 이미지 목록
 * @param createdAt  게시글 생성 일시
 * @param updatedAt  게시글 수정 일시
 */
public record PostResponse(
        Long postId,
        AuthorResponse author,
        String title,
        String content,
        String thumbnailUrl,
        List<CategoryResponse> categories,
        List<ImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post, List<FileAttachment> attachments) {
        return new PostResponse(
                post.getId(),
                AuthorResponse.from(post.getUser()),
                post.getTitle(),
                post.getContent(),
                post.getThumbnailUrl(),
                post.getCategories().stream()
                        .map(CategoryResponse::from)
                        .toList(),
                attachments.stream()
                        .map(ImageResponse::from)
                        .toList(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}