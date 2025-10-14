package com.back.domain.board.comment.controller;

import com.back.domain.board.comment.controller.docs.CommentControllerDocs;
import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.dto.CommentRequest;
import com.back.domain.board.comment.dto.CommentResponse;
import com.back.domain.board.comment.dto.ReplyResponse;
import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.comment.service.CommentService;
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

    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<RsData<PageResponse<CommentListResponse>>> getComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Long userId = (user != null) ? user.getUserId() : null;
        PageResponse<CommentListResponse> response = commentService.getComments(postId, pageable, userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                        "댓글 목록이 조회되었습니다.",
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

    // 대댓글 생성
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<RsData<ReplyResponse>> createReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ReplyResponse response = commentService.createReply(postId, commentId, request, user.getUserId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success(
                        "대댓글이 생성되었습니다.",
                        response
                ));
    }
}
