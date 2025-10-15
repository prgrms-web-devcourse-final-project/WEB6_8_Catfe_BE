package com.back.domain.user.account.controller.docs;

import com.back.domain.board.comment.dto.MyCommentResponse;
import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.user.account.dto.ChangePasswordRequest;
import com.back.domain.user.account.dto.UserProfileRequest;
import com.back.domain.user.account.dto.UserDetailResponse;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User API", description = "사용자 정보 관련 API")
public interface AccountControllerDocs {

    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 사용자의 계정 및 프로필 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "회원 정보를 조회했습니다.",
                                      "data": {
                                        "userId": 1,
                                        "username": "testuser",
                                        "email": "test@example.com",
                                        "role": "USER",
                                        "status": "ACTIVE",
                                        "provider": "LOCAL",
                                        "providerId": null,
                                        "profile": {
                                          "nickname": "홍길동",
                                          "profileImageUrl": "https://cdn.example.com/profile/1.png",
                                          "bio": "안녕하세요! 열심히 배우고 있습니다.",
                                          "birthDate": "2000-01-01",
                                          "point": 1500
                                        },
                                        "createdAt": "2025-09-22T10:00:00",
                                        "updatedAt": "2025-09-22T12:30:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "정지된 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_008",
                                      "message": "정지된 계정입니다. 관리자에게 문의하세요.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탈퇴한 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_009",
                                      "message": "탈퇴한 계정입니다.",
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
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_001",
                                      "message": "존재하지 않는 사용자입니다.",
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
    ResponseEntity<RsData<UserDetailResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "내 정보 수정",
            description = "로그인한 사용자의 프로필 정보를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 정보 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "회원 정보를 수정했습니다.",
                                      "data": {
                                        "userId": 1,
                                        "username": "testuser",
                                        "email": "test@example.com",
                                        "role": "USER",
                                        "status": "ACTIVE",
                                        "provider": "LOCAL",
                                        "providerId": null,
                                        "profile": {
                                          "nickname": "새로운닉네임",
                                          "profileImageUrl": "https://cdn.example.com/profile/new.png",
                                          "bio": "저는 개발 공부 중입니다!",
                                          "birthDate": "2000-05-10",
                                          "point": 1500
                                        },
                                        "createdAt": "2025-09-22T10:00:00",
                                        "updatedAt": "2025-09-22T12:40:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복된 닉네임",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_004",
                                      "message": "이미 사용 중인 닉네임입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "정지된 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_008",
                                      "message": "정지된 계정입니다. 관리자에게 문의하세요.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탈퇴한 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_009",
                                      "message": "탈퇴한 계정입니다.",
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
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_001",
                                      "message": "존재하지 않는 사용자입니다.",
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
    ResponseEntity<RsData<UserDetailResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UserProfileRequest request
    );

    @Operation(
            summary = "비밀번호 변경",
            description = "로그인한 사용자가 본인 계정의 비밀번호를 변경합니다. " +
                    "현재 비밀번호를 검증하고, 새 비밀번호는 정책(최소 8자, 숫자/특수문자 포함)을 만족해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "비밀번호가 변경되었습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "새 비밀번호 정책 위반",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_005",
                                      "message": "비밀번호는 최소 8자 이상, 숫자/특수문자를 포함해야 합니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "소셜 로그인 회원 비밀번호 변경 불가",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_010",
                                      "message": "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "정지된 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_008",
                                      "message": "정지된 계정입니다. 관리자에게 문의하세요.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탈퇴한 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_009",
                                      "message": "탈퇴한 계정입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (토큰 없음/잘못됨/만료 또는 현재 비밀번호 불일치)",
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
                                            """),
                                    @ExampleObject(name = "현재 비밀번호 불일치", value = """
                                            {
                                              "success": false,
                                              "code": "USER_006",
                                              "message": "아이디 또는 비밀번호가 올바르지 않습니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_001",
                                      "message": "존재하지 않는 사용자입니다.",
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
    ResponseEntity<RsData<Void>> changeMyPassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest request
    );

    @Operation(
            summary = "내 계정 삭제",
            description = "로그인한 사용자의 계정을 탈퇴 처리합니다. " +
                    "탈퇴 시 사용자 상태는 DELETED로 변경되며, 프로필 정보는 마스킹 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "회원 탈퇴가 완료되었습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "정지된 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_008",
                                      "message": "정지된 계정입니다. 관리자에게 문의하세요.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탈퇴한 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_009",
                                      "message": "탈퇴한 계정입니다.",
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
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_001",
                                      "message": "존재하지 않는 사용자입니다.",
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
    ResponseEntity<RsData<Void>> deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "내 게시글 목록 조회",
            description = """
                    로그인한 사용자가 작성한 게시글 목록을 조회합니다.
                    - 기본 정렬: createdAt,desc
                    - 페이지 및 정렬 조건은 Query Parameter로 조정 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 게시글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "내 게시글 목록이 조회되었습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "postId": 1,
                                            "author": { "id": 10, "nickname": "홍길동", "profileImageUrl": null },
                                            "title": "첫 글",
                                            "thumbnailUrl": null,
                                            "categories": [
                                              { "id": 1, "name": "프론트엔드", "type": "SUBJECT" }
                                            ],
                                            "likeCount": 5,
                                            "bookmarkCount": 2,
                                            "commentCount": 3,
                                            "createdAt": "2025-09-30T10:15:30",
                                            "updatedAt": "2025-09-30T10:20:00"
                                          },
                                          {
                                            "postId": 2,
                                            "author": { "id": 10, "nickname": "홍길동", "profileImageUrl": null },
                                            "title": "두 번째 글",
                                            "thumbnailUrl": null,
                                            "categories": [],
                                            "likeCount": 0,
                                            "bookmarkCount": 0,
                                            "commentCount": 1,
                                            "createdAt": "2025-09-29T14:00:00",
                                            "updatedAt": "2025-09-29T14:10:00"
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
                    responseCode = "404",
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_001",
                                      "message": "존재하지 않는 사용자입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탈퇴한 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_009",
                                      "message": "탈퇴한 계정입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "정지된 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_008",
                                      "message": "정지된 계정입니다. 관리자에게 문의하세요.",
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
                    responseCode = "400",
                    description = "잘못된 요청(파라미터 오류)",
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
    ResponseEntity<RsData<PageResponse<PostListResponse>>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails user,
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "내 댓글 목록 조회",
            description = """
                    로그인한 사용자가 작성한 댓글 목록을 조회합니다.
                    - 기본 정렬: createdAt,desc
                    - 페이지 및 정렬 조건은 Query Parameter로 조정 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 댓글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "내 댓글 목록이 조회되었습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "commentId": 12,
                                            "postId": 5,
                                            "postTitle": "스프링 트랜잭션 정리",
                                            "parentId": null,
                                            "parentContent": null,
                                            "content": "정말 도움이 많이 됐어요!",
                                            "likeCount": 3,
                                            "createdAt": "2025-09-29T12:15:00",
                                            "updatedAt": "2025-09-29T12:30:00"
                                          },
                                          {
                                            "commentId": 14,
                                            "postId": 5,
                                            "postTitle": "스프링 트랜잭션 정리",
                                            "parentId": 13,
                                            "parentContent": "코딩 박사의 스프링 교재도 추천합니다.",
                                            "content": "감사합니다! 더 공부해볼게요.",
                                            "likeCount": 1,
                                            "createdAt": "2025-09-29T12:45:00",
                                            "updatedAt": "2025-09-29T12:45:00"
                                          }
                                        ],
                                        "page": 0,
                                        "size": 10,
                                        "totalElements": 2,
                                        "totalPages": 1,
                                        "last": true
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_001",
                                      "message": "존재하지 않는 사용자입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탈퇴한 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_009",
                                      "message": "탈퇴한 계정입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "정지된 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_008",
                                      "message": "정지된 계정입니다. 관리자에게 문의하세요.",
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
                    responseCode = "400",
                    description = "잘못된 요청(파라미터 오류)",
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
    ResponseEntity<RsData<PageResponse<MyCommentResponse>>> getMyComments(
            @AuthenticationPrincipal CustomUserDetails user,
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "내 북마크 게시글 목록 조회",
            description = """
                    로그인한 사용자가 북마크한 게시글 목록을 조회합니다.
                    - 기본 정렬: createdAt,desc
                    - 페이지 및 정렬 조건은 Query Parameter로 조정 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 북마크 게시글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "내 북마크 게시글 목록이 조회되었습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "postId": 22,
                                            "author": { "id": 3, "nickname": "홍길동", "profileImageUrl": null },
                                            "title": "JPA 영속성 전이 완벽 정리",
                                            "thumbnailUrl": "https://cdn.example.com/thumbnails/jpa.png",
                                            "categories": [
                                              { "id": 2, "name": "백엔드", "type": "SUBJECT" }
                                            ],
                                            "likeCount": 12,
                                            "bookmarkCount": 7,
                                            "commentCount": 3,
                                            "createdAt": "2025-09-28T11:20:00",
                                            "updatedAt": "2025-09-28T12:00:00"
                                          },
                                          {
                                            "postId": 10,
                                            "author": { "id": 7, "nickname": "이자바", "profileImageUrl": null },
                                            "title": "테스트 코드 작성 가이드",
                                            "thumbnailUrl": null,
                                            "categories": [],
                                            "likeCount": 2,
                                            "bookmarkCount": 1,
                                            "commentCount": 0,
                                            "createdAt": "2025-09-25T09:10:00",
                                            "updatedAt": "2025-09-25T09:10:00"
                                          }
                                        ],
                                        "page": 0,
                                        "size": 10,
                                        "totalElements": 2,
                                        "totalPages": 1,
                                        "last": true
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_001",
                                      "message": "존재하지 않는 사용자입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "탈퇴한 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_009",
                                      "message": "탈퇴한 계정입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "정지된 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_008",
                                      "message": "정지된 계정입니다. 관리자에게 문의하세요.",
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
                    responseCode = "400",
                    description = "잘못된 요청(파라미터 오류)",
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
    ResponseEntity<RsData<PageResponse<PostListResponse>>> getMyBookmarks(
            @AuthenticationPrincipal CustomUserDetails user,
            @ParameterObject Pageable pageable
    );
}
