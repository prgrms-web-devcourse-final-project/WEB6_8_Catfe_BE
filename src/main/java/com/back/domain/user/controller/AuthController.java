package com.back.domain.user.controller;

import com.back.domain.user.dto.*;
import com.back.domain.user.service.AuthService;
import com.back.global.common.dto.RsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {
    private final AuthService authService;

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<RsData<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        UserResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RsData.success(
                        "회원가입이 성공적으로 완료되었습니다. 이메일 인증을 완료해주세요.",
                        response
                ));
    }

    // 이메일 인증
    @GetMapping("/email/verify")
    public ResponseEntity<RsData<UserResponse>> verifyEmail(
            @RequestParam("token") String token
    ) {
        UserResponse userResponse = authService.verifyEmail(token);
        return ResponseEntity
                .ok(RsData.success(
                        "이메일 인증이 완료되었습니다.",
                        userResponse
                ));
    }

    // 인증 메일 재발송
    @PostMapping("/email/verify")
    public ResponseEntity<RsData<Void>> resendVerificationEmail(
            @Valid @RequestBody sendEmailRequest request
    ) {
        authService.resendVerificationEmail(request.email());
        return ResponseEntity
                .ok(RsData.success(
                        "인증 메일이 재발송되었습니다.",
                        null
                ));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<RsData<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = authService.login(request, response);
        return ResponseEntity
                .ok(RsData.success(
                        "로그인에 성공했습니다.",
                        loginResponse
                ));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<RsData<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.logout(request, response);
        return ResponseEntity
                .ok(RsData.success(
                        "로그아웃 되었습니다.",
                        null
                ));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<RsData<Map<String, String>>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String newAccessToken = authService.refreshToken(request, response);
        return ResponseEntity.ok(RsData.success(
                "토큰이 재발급되었습니다.",
                Map.of("accessToken", newAccessToken)
        ));
    }

    @PostMapping("/username/recover")
    public ResponseEntity<RsData<Void>> recoverUsername(
            @Valid @RequestBody sendEmailRequest request
    ) {
        authService.recoverUsername(request.email());
        return ResponseEntity
                .ok(RsData.success(
                        "아이디를 이메일로 전송했습니다.",
                        null
                ));
    }
}
