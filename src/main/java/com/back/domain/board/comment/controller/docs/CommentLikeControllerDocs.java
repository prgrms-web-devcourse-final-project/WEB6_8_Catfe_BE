package com.back.domain.board.comment.controller.docs;

import com.back.domain.board.comment.dto.CommentLikeResponse;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Comment Like API", description = "댓글 좋아요 관련 API")
public interface CommentLikeControllerDocs {

    @Operation(
            summary = "댓글 좋아요 등록",
            description = "로그인한 사용자가 특정 댓글에 좋아요를 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 좋아요 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "댓글 좋아요가 등록되었습니다.",
                                      "data": {
                                        "commentId": 25,
                                        "likeCount": 4
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (파라미터 누락 등)",
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
                    description = "인증 실패 (Access Token 없음/만료/잘못됨)",
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
                    description = "존재하지 않는 사용자 또는 댓글",
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
                    responseCode = "409",
                    description = "이미 좋아요한 댓글",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "COMMENT_005",
                                      "message": "이미 좋아요한 댓글입니다.",
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
    ResponseEntity<RsData<CommentLikeResponse>> likeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "댓글 좋아요 취소",
            description = "로그인한 사용자가 특정 댓글에 등록한 좋아요를 취소합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 좋아요 취소 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "댓글 좋아요가 취소되었습니다.",
                                      "data": {
                                        "commentId": 25,
                                        "likeCount": 3
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (파라미터 누락 등)",
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
                    description = "인증 실패 (Access Token 없음/만료/잘못됨)",
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
                    description = "존재하지 않는 사용자, 댓글 또는 좋아요 기록 없음",
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
                                    @ExampleObject(name = "존재하지 않는 댓글", value = """
                                            {
                                              "success": false,
                                              "code": "COMMENT_001",
                                              "message": "존재하지 않는 댓글입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "좋아요 기록 없음", value = """
                                            {
                                              "success": false,
                                              "code": "COMMENT_006",
                                              "message": "해당 댓글에 대한 좋아요 기록이 없습니다.",
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
    ResponseEntity<RsData<CommentLikeResponse>> cancelLikeComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    );
}
