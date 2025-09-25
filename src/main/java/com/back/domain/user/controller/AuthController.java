package com.back.domain.user.controller;

import com.back.domain.user.dto.LoginRequest;
import com.back.domain.user.dto.UserRegisterRequest;
import com.back.domain.user.dto.UserResponse;
import com.back.domain.user.service.UserService;
import com.back.global.common.dto.RsData;
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
public class AuthController implements AuthControllerDocs {
    private final UserService userService;

    // 회원가입
    @PostMapping("/register")
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

    // 로그인
    @PostMapping("/login")
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

    // 로그아웃
    @PostMapping("/logout")
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
