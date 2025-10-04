package com.back.domain.study.memo.controller;


import com.back.domain.study.memo.dto.MemoRequestDto;
import com.back.domain.study.memo.dto.MemoResponseDto;
import com.back.domain.study.memo.service.MemoService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memos")
public class MemoController {
    private final MemoService memoService;

    // ==================== 생성 및 수정 ===================
    // 메모 생성 및 수정
    @PostMapping
    public ResponseEntity<RsData<MemoResponseDto>> createOrUpdateMemo(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody MemoRequestDto request
    ) {
        Long userId = user.getUserId();
        MemoResponseDto response = memoService.createOrUpdateMemo(userId, request);
        return ResponseEntity.ok(RsData.success("메모가 저장되었습니다.", response));
    }

    // ==================== 조회 ===================
    // 날짜별 메모 조회
    @GetMapping
    public ResponseEntity<RsData<MemoResponseDto>> getMemoByDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = user.getUserId();
        MemoResponseDto response = memoService.getMemoByDate(userId, date);
        return ResponseEntity.ok(RsData.success("메모를 조회했습니다.", response));
    }

    // ==================== 삭제 ===================
    // 메모 삭제
    @DeleteMapping("/{memoId}")
    public ResponseEntity<RsData<MemoResponseDto>> deleteMemo(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long memoId
    ) {
        Long userId = user.getUserId();
        memoService.deleteMemo(userId, memoId);
        MemoResponseDto response = memoService.deleteMemo(userId, memoId);
        return ResponseEntity.ok(RsData.success("메모가 삭제되었습니다.", response));
    }
}
