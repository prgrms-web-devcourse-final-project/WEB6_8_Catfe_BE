package com.back.domain.board.controller;

import com.back.domain.board.dto.CommentRequest;
import com.back.domain.board.dto.CommentResponse;
import com.back.domain.board.service.CommentService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController implements CommentControllerDocs {
    private final CommentService commentService;

    // 댓글 생성
    @PostMapping
    public ResponseEntity<RsData<CommentResponse>> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CommentResponse response = commentService.createComment(postId, request, user.getUserId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success(
                        "댓글이 생성되었습니다.",
                        response
                ));
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<RsData<CommentResponse>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CommentResponse response = commentService.updateComment(postId, commentId, request, user.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                        "댓글이 수정되었습니다.",
                        response
                ));
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<RsData<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        commentService.deleteComment(postId, commentId, user.getUserId());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                   "댓글이 삭제되었습니다.",
                   null
                ));
    }
}
