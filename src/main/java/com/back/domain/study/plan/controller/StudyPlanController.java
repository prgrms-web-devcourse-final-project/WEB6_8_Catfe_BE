package com.back.domain.study.plan.controller;

import com.back.domain.study.plan.dto.StudyPlanRequest;
import com.back.domain.study.plan.dto.StudyPlanListResponse;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.entity.ApplyScope;
import com.back.domain.study.plan.service.StudyPlanService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "StudyPlan", description = "학습 계획 관련 API")
public class StudyPlanController {
    private final StudyPlanService studyPlanService;
    // ==================== 생성 ===================
    @PostMapping
    @Operation( summary = "학습 계획 생성",
            description = "새로운 학습 계획을 생성합니다. 반복 계획 생성 시 최초 계획이 원본 계획으로서 db에 저장되고" +
                    " 이후 반복되는 계획들은 가상 계획으로서 db에는 없지만 조회 시 가상으로 생성됩니다")

    public ResponseEntity<RsData<StudyPlanResponse>> createStudyPlan(
            // 로그인 유저 정보 받기
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyPlanRequest request) {
        //커스텀 디테일 구현 시 사용
        Long userId = user.getUserId();
        StudyPlanResponse response = studyPlanService.createStudyPlan(userId, request);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 생성되었습니다.", response));
    }

    // ==================== 조회 ===================
    // 특정 날짜의 계획들 조회. date 형식: YYYY-MM-DD
    @GetMapping("/date/{date}")
    @Operation(
            summary = "특정 날짜의 학습 계획 조회",
            description = "지정 날짜에 해당하는 모든 학습 계획을 조회합니다."
    )
    public ResponseEntity<RsData<StudyPlanListResponse>> getStudyPlansForDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // 유저 아이디 추출.
        Long userId = user.getUserId();

        List<StudyPlanResponse> plans = studyPlanService.getStudyPlansForDate(userId, date);
        StudyPlanListResponse response = new StudyPlanListResponse(date, plans, plans.size());

        return ResponseEntity.ok(RsData.success("해당 날짜의 계획을 조회했습니다.", response));
    }

    // 기간별 계획 조회. start, end 형식: YYYY-MM-DD
    @GetMapping
    @Operation(
            summary = "기간별 학습 계획 조회",
            description = "기간에 해당하는 모든 학습 계획을 조회합니다."
    )
    public ResponseEntity<RsData<List<StudyPlanResponse>>> getStudyPlansForPeriod(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long userId = user.getUserId();

        List<StudyPlanResponse> plans = studyPlanService.getStudyPlansForPeriod(userId, startDate, endDate);

        return ResponseEntity.ok(RsData.success("기간별 계획을 조회했습니다.", plans));
    }



    // ==================== 수정 ===================
    // 플랜 아이디는 원본의 아이디를 받음
    // 가상인지 원본인지는 서비스에서 원본과 날짜를 대조해 판단
    // 수정 적용 범위를 쿼리 파라미터로 받음 (THIS_ONLY, FROM_THIS_DATE)
    @PutMapping("/{planId}")
    @Operation(
            summary = "학습 계획 수정",
            description = "기존 학습 계획을 수정합니다. 반복 계획의 경우 적용 범위를 applyScope로 설정 할 수 있으며" +
                    "클라이언트에서는 paln에 repeat_rule이 있으면 반복 계획으로 간주하고 반드시 apply_scope를 쿼리 파라미터로 넘겨야 합니다." +
    "repeat_rule이 없으면 단발성 계획으로 간주하여 수정 범위를 설정 할 필요가 없으므로 apply_scope를 넘기지 않아도 됩니다.")
    public ResponseEntity<RsData<StudyPlanResponse>> updateStudyPlan(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId,
            @RequestBody StudyPlanRequest request,
            @RequestParam(required = false, defaultValue = "THIS_ONLY") ApplyScope applyScope) {
        Long userId = user.getUserId();

        StudyPlanResponse response = studyPlanService.updateStudyPlan(userId, planId, request, applyScope);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 수정되었습니다.", response));
    }


    // ==================== 삭제 ===================
    // 플랜 아이디는 원본의 아이디를 받음
    // 가상인지 원본인지는 서비스에서 원본과 날짜를 대조해 판단
    // 삭제 적용 범위를 쿼리 파라미터로 받음 (THIS_ONLY, FROM_THIS_DATE)
    @DeleteMapping("/{planId}")
    @Operation(
            summary = "학습 계획 삭제",
            description = "기존 학습 계획을 삭제합니다. 반복 계획의 경우 적용 범위를 applyScope로 설정 할 수 있으며" +
                    "클라이언트에서는 paln에 repeat_rule이 있으면 반복 계획으로 간주하고 반드시 apply_scope를 쿼리 파라미터로 넘겨야 합니다." +
                    "repeat_rule이 없으면 단발성 계획으로 간주하여 삭제 범위를 설정 할 필요가 없으므로 apply_scope를 넘기지 않아도 됩니다.")
    public ResponseEntity<RsData<Void>> deleteStudyPlan(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long planId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate,
            @RequestParam(required = false) ApplyScope applyScope) {
        Long userId = user.getUserId();

        studyPlanService.deleteStudyPlan(userId, planId, selectedDate, applyScope);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 삭제되었습니다."));
    }

}
