package com.back.domain.board.post.controller;

import com.back.domain.board.post.dto.PostBookmarkResponse;
import com.back.domain.board.post.service.PostBookmarkService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/bookmark")
@RequiredArgsConstructor
public class PostBookmarkController implements PostBookmarkControllerDocs {
    private final PostBookmarkService postBookmarkService;

    // 게시글 북마크
    @PostMapping
    public ResponseEntity<RsData<PostBookmarkResponse>> bookmarkPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PostBookmarkResponse response = postBookmarkService.bookmarkPost(postId, user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                        "게시글 북마크가 등록되었습니다.",
                        response
                ));
    }

    // 게시글 북마크 취소
    @DeleteMapping
    public ResponseEntity<RsData<PostBookmarkResponse>> cancelBookmarkPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PostBookmarkResponse response = postBookmarkService.cancelBookmarkPost(postId, user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                        "게시글 북마크가 취소되었습니다.",
                        response
                ));
    }
}
