package com.back.domain.board.comment.controller.docs;

import com.back.domain.board.comment.dto.CommentListResponse;
import com.back.domain.board.comment.dto.CommentRequest;
import com.back.domain.board.comment.dto.CommentResponse;
import com.back.domain.board.comment.dto.ReplyResponse;
import com.back.domain.board.common.dto.PageResponse;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment API", description = "댓글 관련 API")
public interface CommentControllerDocs {

    @Operation(
            summary = "댓글 생성",
            description = "로그인한 사용자가 특정 게시글에 댓글을 작성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "댓글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "댓글이 생성되었습니다.",
                                      "data": {
                                        "commentId": 25,
                                        "postId": 101,
                                        "author": {
                                          "id": 5,
                                          "nickname": "홍길동",
                                          "profileImageUrl": null
                                        },
                                        "content": "좋은 글 감사합니다!",
                                        "createdAt": "2025-09-22T11:30:00",
                                        "updatedAt": "2025-09-22T11:30:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필드 누락 등)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMON_400",
                                      "message": "잘못된 요청입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 없음/잘못됨/만료)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "토큰 없음", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_001",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "잘못된 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_002",
                                              "message": "유효하지 않은 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "만료된 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_004",
                                              "message": "만료된 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 또는 게시글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "존재하지 않는 사용자", value = """
                                            {
                                              "success": false,
                                              "code": "USER_001",
                                              "message": "존재하지 않는 사용자입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "존재하지 않는 게시글", value = """
                                            {
                                              "success": false,
                                              "code": "POST_001",
                                              "message": "존재하지 않는 게시글입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMON_500",
                                      "message": "서버 오류가 발생했습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<RsData<CommentResponse>> createComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "댓글 목록 조회",
            description = "특정 게시글에 달린 댓글 목록을 조회합니다. " +
                    "부모 댓글 기준으로 페이징되며, 각 댓글의 대댓글(children) 목록이 함께 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "댓글 목록이 조회되었습니다.",
                                      "data": {
                                        "content": [
                                          {
                                            "commentId": 1,
                                            "postId": 101,
                                            "parentId": null,
                                            "author": {
                                              "id": 5,
                                              "nickname": "홍길동",
                                              "profileImageUrl": null
                                            },
                                            "content": "부모 댓글",
                                            "likeCount": 2,
                                            "likedByMe": true,
                                            "createdAt": "2025-09-22T11:30:00",
                                            "updatedAt": "2025-09-22T11:30:00",
                                            "children": [
                                              {
                                                "commentId": 2,
                                                "postId": 101,
                                                "parentId": 1,
                                                "author": {
                                                  "id": 5,
                                                  "nickname": "홍길동",
                                                  "profileImageUrl": null
                                                },
                                                "content": "자식 댓글",
                                                "likeCount": 0,
                                                "likedByMe": false,
                                                "createdAt": "2025-09-22T11:35:00",
                                                "updatedAt": "2025-09-22T11:35:00",
                                                "children": []
                                              }
                                            ]
                                          }
                                        ],
                                        "pageNumber": 0,
                                        "pageSize": 10,
                                        "totalElements": 1,
                                        "totalPages": 1,
                                        "last": true
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (파라미터 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMON_400",
                                      "message": "잘못된 요청입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 게시글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "POST_001",
                                      "message": "존재하지 않는 게시글입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMON_500",
                                      "message": "서버 오류가 발생했습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<RsData<PageResponse<CommentListResponse>>> getComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails user,
            Pageable pageable
    );

    @Operation(
            summary = "댓글 수정",
            description = "로그인한 사용자가 자신이 작성한 댓글을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "댓글이 수정되었습니다.",
                                      "data": {
                                        "commentId": 25,
                                        "postId": 101,
                                        "author": {
                                          "id": 5,
                                          "nickname": "홍길동",
                                          "profileImageUrl": null
                                        },
                                        "content": "수정된 댓글 내용입니다.",
                                        "createdAt": "2025-09-22T11:30:00",
                                        "updatedAt": "2025-09-22T13:00:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필드 누락 등)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMON_400",
                                      "message": "잘못된 요청입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 없음/잘못됨/만료)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "토큰 없음", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_001",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "잘못된 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_002",
                                              "message": "유효하지 않은 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "만료된 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_004",
                                              "message": "만료된 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (작성자 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMENT_002",
                                      "message": "댓글 작성자만 수정/삭제할 수 있습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 게시글 또는 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "존재하지 않는 게시글", value = """
                                            {
                                              "success": false,
                                              "code": "POST_001",
                                              "message": "존재하지 않는 게시글입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "존재하지 않는 댓글", value = """
                                            {
                                              "success": false,
                                              "code": "COMMENT_001",
                                              "message": "존재하지 않는 댓글입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMON_500",
                                      "message": "서버 오류가 발생했습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<RsData<CommentResponse>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "댓글 삭제",
            description = "로그인한 사용자가 자신이 작성한 댓글을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "댓글이 삭제되었습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 없음/잘못됨/만료)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "토큰 없음", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_001",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "잘못된 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_002",
                                              "message": "유효하지 않은 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "만료된 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_004",
                                              "message": "만료된 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (작성자 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMENT_002",
                                      "message": "댓글 작성자만 수정/삭제할 수 있습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 게시글 또는 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "존재하지 않는 게시글", value = """
                                            {
                                              "success": false,
                                              "code": "POST_001",
                                              "message": "존재하지 않는 게시글입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "존재하지 않는 댓글", value = """
                                            {
                                              "success": false,
                                              "code": "COMMENT_001",
                                              "message": "존재하지 않는 댓글입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMON_500",
                                      "message": "서버 오류가 발생했습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<RsData<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "대댓글 생성",
            description = "로그인한 사용자가 특정 게시글의 댓글에 대댓글을 작성합니다. (대댓글은 1단계까지만 허용됩니다.)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "대댓글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "대댓글이 생성되었습니다.",
                                      "data": {
                                        "replyId": 45,
                                        "postId": 101,
                                        "parentId": 25,
                                        "author": {
                                          "id": 7,
                                          "nickname": "이몽룡",
                                          "profileImageUrl": null
                                        },
                                        "content": "저도 동의합니다!",
                                        "createdAt": "2025-09-22T13:30:00",
                                        "updatedAt": "2025-09-22T13:30:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 depth 제한 초과",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "필드 누락", value = """
                                            {
                                              "success": false,
                                              "code": "COMMON_400",
                                              "message": "잘못된 요청입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "부모 댓글 불일치", value = """
                                            {
                                              "success": false,
                                              "code": "COMMENT_003",
                                              "message": "부모 댓글이 해당 게시글에 속하지 않습니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "depth 초과", value = """
                                            {
                                              "success": false,
                                              "code": "COMMENT_004",
                                              "message": "대댓글은 한 단계까지만 작성할 수 있습니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 없음/잘못됨/만료)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "토큰 없음", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_001",
                                              "message": "인증이 필요합니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "잘못된 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_002",
                                              "message": "유효하지 않은 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "만료된 토큰", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_004",
                                              "message": "만료된 액세스 토큰입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 / 게시글 / 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "존재하지 않는 사용자", value = """
                                            {
                                              "success": false,
                                              "code": "USER_001",
                                              "message": "존재하지 않는 사용자입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "존재하지 않는 게시글", value = """
                                            {
                                              "success": false,
                                              "code": "POST_001",
                                              "message": "존재하지 않는 게시글입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "존재하지 않는 댓글", value = """
                                            {
                                              "success": false,
                                              "code": "COMMENT_001",
                                              "message": "존재하지 않는 댓글입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMON_500",
                                      "message": "서버 오류가 발생했습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            )
    })
    ResponseEntity<RsData<ReplyResponse>> createReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );
}