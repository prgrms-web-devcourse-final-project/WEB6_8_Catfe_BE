package com.back.domain.board.post.controller;

import com.back.domain.board.post.dto.PostBookmarkResponse;
import com.back.domain.board.post.service.PostBookmarkService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/bookmark")
@RequiredArgsConstructor
public class PostBookmarkController {
    private final PostBookmarkService bookmarkService;
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
}
