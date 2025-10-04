package com.back.domain.study.plan.dto;

import com.back.domain.study.plan.entity.Color;
import com.back.domain.study.plan.entity.DayOfWeek;
import com.back.domain.study.plan.entity.Frequency;
import com.back.domain.study.plan.entity.StudyPlan;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanResponse {
    private Long id;
    private String subject;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    private Color color;

    // RepeatRule 정보
    private RepeatRuleResponse repeatRule;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepeatRuleResponse {
        private Frequency frequency;
        private Integer intervalValue;
        private List<DayOfWeek> byDay = new ArrayList<>();  // "MON" 형태의 enum 문자열 리스트

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate untilDate;

        // RepeatRule 엔티티를 DTO로 변환하는 생성자
        // intervalValue의 경우 요청, 응답(프론트)에서는 intervalValue 사용
        // 백엔드 내에서는 repeatInterval 사용
        public RepeatRuleResponse(com.back.domain.study.plan.entity.RepeatRule repeatRule) {
            if (repeatRule != null) {
                this.frequency = repeatRule.getFrequency();
                this.intervalValue = repeatRule.getRepeatInterval();
                this.byDay = repeatRule.getByDay();
                this.untilDate = repeatRule.getUntilDate();
            }
        }

    }
    // 엔티티를 DTO로 변환하는 생성자
    public StudyPlanResponse(StudyPlan studyPlan) {
        if (studyPlan != null) {
            this.id = studyPlan.getId();
            this.subject = studyPlan.getSubject();
            this.startDate = studyPlan.getStartDate();
            this.endDate = studyPlan.getEndDate();
            this.color = studyPlan.getColor();


            // RepeatRule 변환
            if (studyPlan.getRepeatRule() != null) {
                this.repeatRule = new RepeatRuleResponse(studyPlan.getRepeatRule());
            }
        }
    }
}