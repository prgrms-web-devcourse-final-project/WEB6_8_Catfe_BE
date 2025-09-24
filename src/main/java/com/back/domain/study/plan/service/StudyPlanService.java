package com.back.domain.study.plan.service;

import com.back.domain.study.plan.dto.StudyPlanCreateRequest;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.entity.RepeatRule;
import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.plan.entity.StudyStatus;
import com.back.domain.study.plan.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPlanService{
    private final StudyPlanRepository studyPlanRepository;

    @Transactional
    public StudyPlanResponse createStudyPlan(Long userId, StudyPlanCreateRequest request) {

        StudyPlan studyPlan = new StudyPlan();
        //studyPlan.setUser(user);
        studyPlan.setSubject(request.getSubject());
        studyPlan.setStatus(StudyStatus.TODO);
        studyPlan.setStartDate(request.getStartDate());
        studyPlan.setEndDate(request.getEndDate());
        studyPlan.setColor(request.getColor());
        studyPlan.setParentPlan(null);

        // 반복 규칙 설정
        if (request.getRepeatRule() != null) {
            StudyPlanCreateRequest.RepeatRuleRequest repeatRuleRequest = request.getRepeatRule();
            RepeatRule repeatRule = new RepeatRule();
            repeatRule.setStudyPlan(studyPlan);
            repeatRule.setFrequency(repeatRuleRequest.getFrequency());
            repeatRule.setRepeatInterval(repeatRuleRequest.getIntervalValue() != null ? repeatRuleRequest.getIntervalValue() : 1);

            // byDay 문자열 그대로 저장
            if (repeatRuleRequest.getByDay() != null && !repeatRuleRequest.getByDay().isEmpty()) {
                repeatRule.setByDay(repeatRuleRequest.getByDay());
            }

            // untilDate 문자열을 LocalDateTime으로 변환
            if (repeatRuleRequest.getUntilDate() != null && !repeatRuleRequest.getUntilDate().isEmpty()) {
                try {
                    LocalDateTime untilDateTime = LocalDateTime.parse(repeatRuleRequest.getUntilDate() + "T23:59:59");
                    repeatRule.setUntilDate(untilDateTime);
                } catch (Exception e) {
                    // 날짜 파싱 실패 시 무시하거나 예외 처리
                }
            }

            studyPlan.setRepeatRule(repeatRule);
        }

        StudyPlan savedStudyPlan = studyPlanRepository.save(studyPlan);
        return convertToResponse(savedStudyPlan);
    }



}
