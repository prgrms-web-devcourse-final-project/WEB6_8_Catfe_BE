package com.back.domain.board.post.controller;

import com.back.domain.board.post.dto.PostLikeResponse;
import com.back.domain.board.post.service.PostLikeService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/like")
@RequiredArgsConstructor
public class PostLikeController {
    private final PostLikeService postLikeService;

    // 게시글 좋아요
    @GetMapping
    public ResponseEntity<RsData<PostLikeResponse>> likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PostLikeResponse response = postLikeService.likePost(postId, user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                        "게시글 좋아요가 등록되었습니다.",
                        response
                ));
    }
}
