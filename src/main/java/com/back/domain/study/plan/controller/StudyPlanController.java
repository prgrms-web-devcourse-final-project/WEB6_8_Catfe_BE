package com.back.domain.study.plan.controller;

import com.back.domain.study.plan.dto.StudyPlanCreateRequest;
import com.back.domain.study.plan.dto.StudyPlanListResponse;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.service.StudyPlanService;
import com.back.global.common.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class StudyPlanController {
    private final StudyPlanService studyPlanService;
    // ==================== 생성 ===================
    @PostMapping
    public ResponseEntity<RsData<StudyPlanResponse>> createStudyPlan(
            // 로그인 유저 정보 받기 @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyPlanCreateRequest request) {
        //커스텀 디테일 구현 시 사용 int userId = user.getId();
        Long userId = 1L; // 임시로 userId를 1로 설정
        StudyPlanResponse response = studyPlanService.createStudyPlan(userId, request);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 생성되었습니다.", response));
    }

    // ==================== 조회 ===================
    // 특정 날짜의 계획들 조회. date 형식: YYYY-MM-DD
    @GetMapping("/date/{date}")
    public ResponseEntity<RsData<StudyPlanListResponse>> getStudyPlansForDate(
            // @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // 유저 아이디 추출. 지금은 임시값 적용
        // Long userId = user.getId();
        Long userId = 1L; // 임시로 userId를 1로 설정

        List<StudyPlanResponse> plans = studyPlanService.getStudyPlansForDate(userId, date);
        StudyPlanListResponse response = new StudyPlanListResponse(date, plans, plans.size());

        return ResponseEntity.ok(RsData.success("해당 날짜의 계획을 조회했습니다.", response));
    }

    // 기간별 계획 조회
    @GetMapping
    public ResponseEntity<RsData<List<StudyPlanResponse>>> getStudyPlansForPeriod(
            // @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Long userId = user.getId();
        Long userId = 1L; // 임시

        List<StudyPlanResponse> plans = studyPlanService.getStudyPlansForPeriod(userId, startDate, endDate);

        return ResponseEntity.ok(RsData.success("기간별 계획을 조회했습니다.", plans));
    }



    // ==================== 수정 ===================



    // ==================== 삭제 ===================
    @DeleteMapping("/{planId}")
    public ResponseEntity<RsData<Void>> deleteStudyPlan(@PathVariable Long planId) {
        //studyPlanService.deleteStudyPlan(planId);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 삭제되었습니다.", null));
    }



}
