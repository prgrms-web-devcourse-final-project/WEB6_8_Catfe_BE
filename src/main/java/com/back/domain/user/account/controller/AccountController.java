package com.back.domain.user.account.controller;

import com.back.domain.board.comment.dto.MyCommentResponse;
import com.back.domain.board.common.dto.PageResponse;
import com.back.domain.board.post.dto.PostListResponse;
import com.back.domain.user.account.controller.docs.AccountControllerDocs;
import com.back.domain.user.account.dto.ChangePasswordRequest;
import com.back.domain.user.account.dto.UpdateUserProfileRequest;
import com.back.domain.user.account.dto.UserDetailResponse;
import com.back.domain.user.account.service.AccountService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class AccountController implements AccountControllerDocs {
    private final AccountService accountService;

    // 내 정보 조회
    @GetMapping
    public ResponseEntity<RsData<UserDetailResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        UserDetailResponse userDetail = accountService.getUserInfo(user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                        "회원 정보를 조회했습니다.",
                        userDetail
                ));
    }

    // 내 정보 수정
    @PatchMapping
    public ResponseEntity<RsData<UserDetailResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserDetailResponse updated = accountService.updateUserProfile(user.getUserId(), request);
        return ResponseEntity
                .ok(RsData.success(
                        "회원 정보를 수정했습니다.",
                        updated
                ));
    }

    // 내 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<RsData<Void>> changeMyPassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        accountService.changePassword(user.getUserId(), request);
        return ResponseEntity
                .ok(RsData.success(
                        "비밀번호가 변경되었습니다."
                ));
    }

    // 내 계정 삭제
    @DeleteMapping
    public ResponseEntity<RsData<Void>> deleteMyAccount(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        accountService.deleteUser(user.getUserId());
        return ResponseEntity
                .ok(RsData.success(
                        "회원 탈퇴가 완료되었습니다."
                ));
    }

    // 내 게시글 목록 조회
    @GetMapping("/posts")
    public ResponseEntity<RsData<PageResponse<PostListResponse>>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PostListResponse> response = accountService.getMyPosts(user.getUserId(), pageable);
        return ResponseEntity
                .ok(RsData.success(
                        "내 게시글 목록이 조회되었습니다.",
                        response
                ));
    }

    // 내 댓글 목록 조회
    @GetMapping("/comments")
    public ResponseEntity<RsData<PageResponse<MyCommentResponse>>> getMyComments(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<MyCommentResponse> response = accountService.getMyComments(user.getUserId(), pageable);
        return ResponseEntity
                .ok(RsData.success(
                        "내 댓글 목록이 조회되었습니다.",
                        response
                ));
    }

    // 내 북마크 게시글 목록 조회
    @GetMapping("/bookmarks")
    public ResponseEntity<RsData<PageResponse<PostListResponse>>> getMyBookmarks(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PostListResponse> response = accountService.getMyBookmarks(user.getUserId(), pageable);
        return ResponseEntity
                .ok(RsData.success(
                        "내 북마크 게시글 목록이 조회되었습니다.",
                        response
                ));
    }
}
