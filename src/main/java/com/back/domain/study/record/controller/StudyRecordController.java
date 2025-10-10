package com.back.domain.study.record.controller;

import com.back.domain.study.record.dto.StudyRecordRequestDto;
import com.back.domain.study.record.dto.StudyRecordResponseDto;
import com.back.domain.study.record.service.StudyRecordService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/plans/records")
@Tag(name = "StudyRecord", description = "학습 조회 관련 API")
public class StudyRecordController {
    private final StudyRecordService studyRecordService;

    // ======================= 생성 ======================
    // 학습 기록 생성
    @PostMapping
    @Operation( summary = "학습 기록 생성",
            description = "기록을 생성합니다.")
    public ResponseEntity<RsData<StudyRecordResponseDto>> createStudyRecord(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody StudyRecordRequestDto request
    ) {
        Long userId = user.getUserId();
        StudyRecordResponseDto response = studyRecordService.createStudyRecord(userId, request);
        return ResponseEntity.ok(RsData.success("학습 기록이 생성되었습니다.", response));
    }
    // ======================= 조회 ======================
    // 일별 학습 기록 조회
    @GetMapping
    @Operation( summary = "학습 기록 조회",
            description = "날짜별 기록을 조회합니다.")
    public ResponseEntity<RsData<List<StudyRecordResponseDto>>> getDailyStudyRecord(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = user.getUserId();
        List<StudyRecordResponseDto> response = studyRecordService.getStudyRecordsByDate(userId, date);
        return ResponseEntity.ok(RsData.success("일별 학습 기록 조회 성공", response));
    }
}
