package com.back.domain.user.controller;

import com.back.domain.user.dto.UpdateUserProfileRequest;
import com.back.domain.user.dto.UserDetailResponse;
import com.back.domain.user.service.UserService;
import com.back.global.common.dto.RsData;
import com.back.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<RsData<UserDetailResponse>> getMyInfo (
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        UserDetailResponse userDetail = userService.getUserInfo(user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                        "회원 정보를 조회했습니다.",
                        userDetail
                ));
    }

    @PatchMapping("/me")
    public ResponseEntity<RsData<UserDetailResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserDetailResponse updated = userService.updateUserProfile(user.getUserId(), request);
        return ResponseEntity
                .ok(RsData.success(
                        "회원 정보를 수정했습니다.",
                        updated
                )
        );
    }
}
