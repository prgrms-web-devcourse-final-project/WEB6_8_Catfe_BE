package com.back.domain.board.post.controller;

import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.controller.docs.PostControllerDocs;
import com.back.domain.board.post.dto.PostDetailResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.board.post.dto.PostRequest;
import com.back.domain.board.post.dto.PostResponse;
import com.back.domain.board.post.service.PostService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController implements PostControllerDocs {
    private final PostService postService;

    // 게시글 생성
    @PostMapping
    public ResponseEntity<RsData<PostResponse>> createPost(
            @RequestBody @Valid PostRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PostResponse response = postService.createPost(request, user.getUserId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success(
                        "게시글이 생성되었습니다.",
                        response
                ));
    }

    // 게시글 다건 조회
    @GetMapping
    public ResponseEntity<RsData<PageResponse<PostListResponse>>> getPosts(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) List<Long> categoryId
    ) {
        PageResponse<PostListResponse> response = postService.getPosts(keyword, searchType, categoryId, pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                        "게시글 목록이 조회되었습니다.",
                        response
                ));
    }

    // 게시글 단건 조회
    @GetMapping("/{postId}")
    public ResponseEntity<RsData<PostDetailResponse>> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long userId = (user != null) ? user.getUserId() : null;
        PostDetailResponse response = postService.getPost(postId, userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                        "게시글이 조회되었습니다.",
                        response
                ));
    }

    // 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<RsData<PostResponse>> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid PostRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PostResponse response = postService.updatePost(postId, request, user.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                        "게시글이 수정되었습니다.",
                        response
                ));
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<RsData<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        postService.deletePost(postId, user.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                        "게시글이 삭제되었습니다.",
                        null
                ));
    }
}