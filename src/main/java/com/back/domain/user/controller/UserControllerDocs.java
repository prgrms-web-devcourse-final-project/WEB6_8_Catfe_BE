package com.back.domain.user.controller;

import com.back.domain.user.dto.ChangePasswordRequest;
import com.back.domain.user.dto.UpdateUserProfileRequest;
import com.back.domain.user.dto.UserDetailResponse;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User API", description = "사용자 정보 관련 API")
public interface UserControllerDocs {

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
            @Valid @RequestBody UpdateUserProfileRequest request
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
}
