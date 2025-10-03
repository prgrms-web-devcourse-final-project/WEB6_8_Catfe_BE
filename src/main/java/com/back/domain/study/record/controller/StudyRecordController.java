package com.back.domain.study.record.controller;

import com.back.domain.study.record.dto.StudyRecordRequestDto;
import com.back.domain.study.record.dto.StudyRecordResponseDto;
import com.back.domain.study.record.service.StudyRecordService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans/records")
public class StudyRecordController {
    private final StudyRecordService studyRecordService;

    // 학습 기록 생성
    @PostMapping
    public ResponseEntity<RsData<StudyRecordResponseDto>> createStudyRecord(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody StudyRecordRequestDto request
    ) {
        Long userId = user.getUserId();
        StudyRecordResponseDto response = studyRecordService.createStudyRecord(userId, request);
        return ResponseEntity.ok(RsData.success("학습 기록이 생성되었습니다.", response));
    }

    @GetMapping("date/{date}")
    public ResponseEntity<RsData<List<StudyRecordResponseDto>>> getDailyStudyRecord(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("date") LocalDate date
    ) {
        Long userId = user.getUserId();
        List<StudyRecordResponseDto> response = studyRecordService.getStudyRecordsByDate(userId, date);
        return ResponseEntity.ok(RsData.success("일별 학습 기록 조회 성공", response));
    }
}
