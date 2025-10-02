package com.back.domain.board.controller;

import com.back.domain.board.dto.*;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Post API", description = "게시글 관련 API")
public interface PostControllerDocs {

    @Operation(
            summary = "게시글 생성",
            description = "로그인한 사용자가 새 게시글을 작성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "게시글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "게시글이 생성되었습니다.",
                                      "data": {
                                        "postId": 101,
                                        "author": {
                                          "id": 5,
                                          "nickname": "홍길동"
                                        },
                                        "title": "첫 번째 게시글",
                                        "content": "안녕하세요, 첫 글입니다!",
                                        "categories": [
                                          { "id": 1, "name": "공지사항" },
                                          { "id": 2, "name": "자유게시판" }
                                        ],
                                        "createdAt": "2025-09-22T10:30:00",
                                        "updatedAt": "2025-09-22T10:30:00"
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
                    description = "존재하지 않는 사용자 또는 카테고리",
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
                                    @ExampleObject(name = "존재하지 않는 카테고리", value = """
                                            {
                                              "success": false,
                                              "code": "POST_003",
                                              "message": "존재하지 않는 카테고리입니다.",
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
    ResponseEntity<RsData<PostResponse>> createPost(
            @RequestBody PostRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "게시글 목록 조회",
            description = "모든 사용자가 게시글 목록을 조회할 수 있습니다. (로그인 불필요)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "게시글 목록이 조회되었습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "postId": 1,
                                            "author": { "id": 10, "nickname": "홍길동" },
                                            "title": "첫 글",
                                            "categories": [{ "id": 1, "name": "공지사항" }],
                                            "likeCount": 5,
                                            "bookmarkCount": 2,
                                            "commentCount": 3,
                                            "createdAt": "2025-09-30T10:15:30",
                                            "updatedAt": "2025-09-30T10:20:00"
                                          }
                                        ],
                                        "page": 0,
                                        "size": 10,
                                        "totalElements": 25,
                                        "totalPages": 3,
                                        "last": false
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (페이징 파라미터 오류 등)",
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
    ResponseEntity<RsData<PageResponse<PostListResponse>>> getPosts(
            @PageableDefault(sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) Long categoryId
    );


    @Operation(
            summary = "게시글 단건 조회",
            description = "모든 사용자가 특정 게시글의 상세 정보를 조회할 수 있습니다. (로그인 불필요)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 단건 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "게시글이 조회되었습니다.",
                                      "data": {
                                        "postId": 101,
                                        "author": { "id": 5, "nickname": "홍길동" },
                                        "title": "첫 번째 게시글",
                                        "content": "안녕하세요, 첫 글입니다!",
                                        "categories": [
                                          { "id": 1, "name": "공지사항" },
                                          { "id": 2, "name": "자유게시판" }
                                        ],
                                        "likeCount": 10,
                                        "bookmarkCount": 2,
                                        "commentCount": 3,
                                        "createdAt": "2025-09-22T10:30:00",
                                        "updatedAt": "2025-09-22T10:30:00"
                                      }
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
    ResponseEntity<RsData<PostDetailResponse>> getPost(
            @PathVariable Long postId
    );

    @Operation(
            summary = "게시글 수정",
            description = "로그인한 사용자가 자신의 게시글을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "게시글이 수정되었습니다.",
                                      "data": {
                                        "postId": 101,
                                        "author": {
                                          "id": 5,
                                          "nickname": "홍길동"
                                        },
                                        "title": "수정된 게시글",
                                        "content": "안녕하세요, 수정했습니다!",
                                        "categories": [
                                          { "id": 1, "name": "공지사항" },
                                          { "id": 2, "name": "자유게시판" }
                                        ],
                                        "createdAt": "2025-09-22T10:30:00",
                                        "updatedAt": "2025-09-22T10:30:00"
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
                    description = "인증 실패 (토큰 없음/만료/잘못됨)",
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
                                      "code": "POST_002",
                                      "message": "게시글 작성자만 수정/삭제할 수 있습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 게시글 또는 카테고리",
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
                                    @ExampleObject(name = "존재하지 않는 카테고리", value = """
                                            {
                                              "success": false,
                                              "code": "POST_003",
                                              "message": "존재하지 않는 카테고리입니다.",
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
    ResponseEntity<RsData<PostResponse>> updatePost(
            @PathVariable Long postId,
            @RequestBody PostRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    );
}
