package com.back.domain.board.comment.controller;

import com.back.domain.board.comment.controller.docs.CommentLikeControllerDocs;
import com.back.domain.board.comment.dto.CommentLikeResponse;
import com.back.domain.board.comment.service.CommentLikeService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/comments/{commentId}/like")
@RequiredArgsConstructor
public class CommentLikeController implements CommentLikeControllerDocs {
    private final CommentLikeService commentLikeService;

    // 댓글 좋아요
    @PostMapping
    public ResponseEntity<RsData<CommentLikeResponse>> likeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CommentLikeResponse response = commentLikeService.likeComment(commentId, user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                "댓글 좋아요가 등록되었습니다.",
                        response
                ));
    }

    // 댓글 좋아요 취소
    @DeleteMapping
    public ResponseEntity<RsData<CommentLikeResponse>> cancelLikeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CommentLikeResponse response = commentLikeService.cancelLikeComment(commentId, user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                        "댓글 좋아요가 취소되었습니다.",
                        response
                ));
    }
}
