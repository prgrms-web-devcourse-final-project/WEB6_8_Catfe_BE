package com.back.domain.user.controller;

import com.back.domain.user.dto.ChangePasswordRequest;
import com.back.domain.user.dto.UpdateUserProfileRequest;
import com.back.domain.user.dto.UserDetailResponse;
import com.back.domain.user.service.UserService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {
    private final UserService userService;

    // 내 정보 조회
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

    // 내 정보 수정
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
                ));
    }

    // 내 비밀번호 변경
    @PatchMapping("/me/password")
    public ResponseEntity<RsData<Void>> changeMyPassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(user.getUserId(), request);
        return ResponseEntity
                .ok(RsData.success(
                        "비밀번호가 변경되었습니다."
                ));
    }

    // 내 계정 삭제
    @DeleteMapping("/me")
    public ResponseEntity<RsData<Void>> deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        userService.deleteUser(user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                        "회원 탈퇴가 완료되었습니다."
                ));
    }
}
