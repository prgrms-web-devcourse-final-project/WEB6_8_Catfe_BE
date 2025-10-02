package com.back.domain.board.controller;

import com.back.domain.board.dto.CommentRequest;
import com.back.domain.board.dto.CommentResponse;
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
                                          "nickname": "홍길동"
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
}