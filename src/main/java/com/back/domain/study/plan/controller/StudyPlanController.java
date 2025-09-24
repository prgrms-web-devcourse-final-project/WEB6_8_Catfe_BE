package com.back.domain.study.plan.controller;

import com.back.domain.study.plan.dto.StudyPlanCreateRequest;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.service.StudyPlanService;
import com.back.global.common.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class StudyPlanController {
    private final StudyPlanService studyPlanService;

    @PostMapping
    public ResponseEntity<RsData<StudyPlanResponse>> createStudyPlan(
            // 로그인 유저 정보 받기 @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody StudyPlanCreateRequest request) {
        //커스텀 디테일 구현 시 사용 int userId = user.getId();
        Long userId = 1L; // 임시로 userId를 1로 설정
        StudyPlanResponse response = studyPlanService.createStudyPlan(userId, request);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 생성되었습니다.", response));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<RsData<Void>> deleteStudyPlan(@PathVariable Long planId) {
        //studyPlanService.deleteStudyPlan(planId);
        return ResponseEntity.ok(RsData.success("학습 계획이 성공적으로 삭제되었습니다.", null));
    }




}
