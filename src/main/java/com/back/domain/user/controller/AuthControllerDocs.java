package com.back.domain.user.controller;

import com.back.domain.user.dto.LoginRequest;
import com.back.domain.user.dto.LoginResponse;
import com.back.domain.user.dto.UserRegisterRequest;
import com.back.domain.user.dto.UserResponse;
import com.back.global.common.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Tag(name = "Auth API", description = "인증/인가 관련 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "회원가입",
            description = "신규 사용자를 등록합니다. " +
                    "회원가입 시 기본 상태는 `PENDING`이며, 추후 이메일 인증 완료 시 `ACTIVE`로 변경됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "회원가입이 성공적으로 완료되었습니다. 이메일 인증을 완료해주세요.",
                                      "data": {
                                        "userId": 1,
                                        "username": "testuser",
                                        "email": "test@example.com",
                                        "nickname": "홍길동",
                                        "role": "USER",
                                        "status": "PENDING",
                                        "createdAt": "2025-09-19T15:00:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복된 아이디/이메일/닉네임",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "중복 아이디", value = """
                                            {
                                              "success": false,
                                              "code": "USER_002",
                                              "message": "이미 사용 중인 아이디입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "중복 이메일", value = """
                                            {
                                              "success": false,
                                              "code": "USER_003",
                                              "message": "이미 사용 중인 이메일입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "중복 닉네임", value = """
                                            {
                                              "success": false,
                                              "code": "USER_004",
                                              "message": "이미 사용 중인 닉네임입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 / 비밀번호 정책 위반",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "비밀번호 정책 위반", value = """
                                            {
                                              "success": false,
                                              "code": "USER_005",
                                              "message": "비밀번호는 최소 8자 이상, 숫자/특수문자를 포함해야 합니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "잘못된 요청", value = """
                                            {
                                              "success": false,
                                              "code": "COMMON_400",
                                              "message": "잘못된 요청입니다.",
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
    ResponseEntity<RsData<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest request
    );

    @Operation(
            summary = "로그인",
            description = "username + password로 로그인합니다. " +
                    "로그인 성공 시 Access Token을 응답 본문에, Refresh Token을 HttpOnly 쿠키에 담아 반환합니다. "
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "로그인에 성공했습니다.",
                                      "data": {
                                        "accessToken": "{accessToken}",
                                        "user": {
                                          "userId": 1,
                                          "username": "testuser",
                                          "email": "test@example.com",
                                          "nickname": "홍길동",
                                          "role": "USER",
                                          "status": "ACTIVE",
                                          "createdAt": "2025-09-19T15:00:00"
                                        }
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "잘못된 아이디/비밀번호",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "USER_006",
                                      "message": "아이디 또는 비밀번호가 올바르지 않습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "이메일 미인증 / 정지 계정",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "이메일 미인증", value = """
                                            {
                                              "success": false,
                                              "code": "USER_007",
                                              "message": "이메일 인증 후 로그인할 수 있습니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "정지 계정", value = """
                                            {
                                              "success": false,
                                              "code": "USER_008",
                                              "message": "정지된 계정입니다. 관리자에게 문의하세요.",
                                              "data": null
                                            }
                                            """)
                            }
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
    ResponseEntity<RsData<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "소셜 로그인",
            description = "카카오/구글/네이버 등의 소셜 로그인을 수행합니다. " +
                    "로그인 성공 시 Access Token을 응답 본문에, Refresh Token을 HttpOnly 쿠키에 담아 반환합니다. " +
                    "요청 경로: /oauth2/authorization/{provider} (예: /oauth2/authorization/kakao)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "소셜 로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "소셜 로그인에 성공했습니다.",
                                      "data": {
                                        "accessToken": "{accessToken}",
                                        "user": {
                                          "userId": 1,
                                          "username": "kakao_1234567890",
                                          "email": "test@kakao.com",
                                          "nickname": "홍길동",
                                          "role": "USER",
                                          "status": "ACTIVE",
                                          "createdAt": "2025-09-27T15:00:00"
                                        }
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 Provider",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "code": "AUTH_008",
                                      "message": "지원하지 않는 소셜 로그인 제공자입니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "소셜 계정 정보 부족 / 인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "필수 정보 누락", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_009",
                                              "message": "소셜 계정에서 필요한 사용자 정보를 가져올 수 없습니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "인증 처리 실패", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_010",
                                              "message": "소셜 로그인 인증에 실패했습니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "DB 사용자 조회 실패",
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
    @GetMapping("/docs/oauth2/authorization/{provider}")
    default ResponseEntity<RsData<LoginResponse>> oauth2LoginDocs() {
        throw new UnsupportedOperationException("Swagger 문서 전용 엔드포인트입니다.");
    }

    @Operation(
            summary = "로그아웃",
            description = "사용자의 Refresh Token을 무효화하여 더 이상 토큰 재발급이 불가능하게 합니다. " +
                    "Access Token은 클라이언트(프론트엔드) 메모리에서 삭제해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "로그아웃 되었습니다.",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "만료 또는 유효하지 않은 Refresh Token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "만료된 Refresh Token", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_005",
                                              "message": "만료된 리프레시 토큰입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "유효하지 않은 Refresh Token", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_003",
                                              "message": "유효하지 않은 리프레시 토큰입니다.",
                                              "data": null
                                            }
                                            """)
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (토큰 없음 / 형식 오류)",
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
    ResponseEntity<RsData<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "토큰 재발급",
            description = "만료된 Access Token 대신 Refresh Token을 이용해 새로운 Access Token을 발급받습니다. " +
                    "Refresh Token은 HttpOnly 쿠키에서 추출하며, 재발급 성공 시 본문에 새로운 Access Token을 담습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUCCESS_200",
                                      "message": "토큰이 재발급되었습니다.",
                                      "data": {
                                        "accessToken": "{newAccessToken}"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh Token 없음 / 잘못된 요청",
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
                    description = "Refresh Token 만료 또는 위조/무효",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Refresh Token 만료", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_005",
                                              "message": "만료된 리프레시 토큰입니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "Refresh Token 위조/무효", value = """
                                            {
                                              "success": false,
                                              "code": "AUTH_003",
                                              "message": "유효하지 않은 리프레시 토큰입니다.",
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
    ResponseEntity<RsData<Map<String, String>>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    );
}
