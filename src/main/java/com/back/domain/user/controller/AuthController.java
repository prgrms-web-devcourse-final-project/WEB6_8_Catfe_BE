package com.back.domain.user.controller;

import com.back.domain.user.dto.LoginRequest;
import com.back.domain.user.dto.UserRegisterRequest;
import com.back.domain.user.dto.UserResponse;
import com.back.domain.user.service.UserService;
import com.back.global.common.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    @Operation(
            summary = "회원가입",
            description = "신규 사용자를 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 / 비밀번호 정책 위반"),
            @ApiResponse(responseCode = "409", description = "중복된 아이디/이메일/닉네임"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<RsData<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        UserResponse response = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success(
                        "회원가입이 성공적으로 완료되었습니다. 이메일 인증을 완료해주세요.",
                        response
                ));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "username + password로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "잘못된 아이디/비밀번호"),
            @ApiResponse(responseCode = "403", description = "이메일 미인증/정지 계정"),
            @ApiResponse(responseCode = "410", description = "탈퇴한 계정")
    })
    public ResponseEntity<RsData<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        UserResponse loginResponse = userService.login(request, response);
        return ResponseEntity
                .ok(RsData.success(
                        "로그인에 성공했습니다.",
                        loginResponse
                ));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "이미 만료되었거나 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<RsData<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        userService.logout(request, response);
        return ResponseEntity
                .ok(RsData.success(
                        "로그아웃 되었습니다.",
                        null
                ));
    }
}
