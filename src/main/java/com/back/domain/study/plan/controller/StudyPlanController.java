package com.back.domain.study.plan.controller;

import com.back.domain.study.plan.dto.StudyPlanRequest;
import com.back.domain.study.plan.dto.StudyPlanListResponse;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.entity.StudyPlanException;
import com.back.domain.study.plan.service.StudyPlanService;
import com.back.global.common.dto.RsData;
import com.back.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class StudyPlanController {
    private final StudyPlanService studyPlanService;
    // ==================== 생성 ===================
    @PostMapping
    public ResponseEntity<RsData<StudyPlanResponse>> createStudyPlan(
            // 로그인 유저 정보 받기 @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyPlanRequest request) {
        //커스텀 디테일 구현 시 사용 int userId = user.getId();
        Long userId = 1L; // 임시로 userId를 1로 설정
        StudyPlanResponse response = studyPlanService.createStudyPlan(userId, request);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 생성되었습니다.", response));
    }

    // ==================== 조회 ===================
    // 특정 날짜의 계획들 조회. date 형식: YYYY-MM-DD
    @GetMapping("/date/{date}")
    public ResponseEntity<RsData<StudyPlanListResponse>> getStudyPlansForDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // 유저 아이디 추출. 지금은 임시값 적용
        // Long userId = user.getId();
        Long userId = 1L; // 임시로 userId를 1로 설정

        List<StudyPlanResponse> plans = studyPlanService.getStudyPlansForDate(userId, date);
        StudyPlanListResponse response = new StudyPlanListResponse(date, plans, plans.size());

        return ResponseEntity.ok(RsData.success("해당 날짜의 계획을 조회했습니다.", response));
    }

    // 기간별 계획 조회. start, end 형식: YYYY-MM-DD
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
    // 플랜 아이디는 원본의 아이디를 받음
    // 가상인지 원본인지는 서비스에서 원본과 날짜를 대조해 판단
    // 수정 적용 범위를 쿼리 파라미터로 받음 (THIS_ONLY, FROM_THIS_DATE)
    @PutMapping("/{planId}")
    public ResponseEntity<RsData<StudyPlanResponse>> updateStudyPlan(
            // @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId,
            @RequestBody StudyPlanRequest request,
            @RequestParam(required = false, defaultValue = "THIS_ONLY") StudyPlanException.ApplyScope applyScope) {
        // Long userId = user.getId();
        Long userId = 1L; // 임시

        StudyPlanResponse response = studyPlanService.updateStudyPlan(userId, planId, request, applyScope);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 수정되었습니다.", response));
    }



    // ==================== 삭제 ===================
    @DeleteMapping("/{planId}")
    public ResponseEntity<RsData<Void>> deleteStudyPlan(@PathVariable Long planId) {
        //studyPlanService.deleteStudyPlan(planId);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 삭제되었습니다.", null));
    }



}
