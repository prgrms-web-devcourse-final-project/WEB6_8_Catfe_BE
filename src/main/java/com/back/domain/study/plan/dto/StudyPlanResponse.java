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
        private Integer repeatInterval;
        private String byDay; // "MON" 형태의 문자열

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDate untilDate;

        public RepeatRuleResponse(com.back.domain.study.plan.entity.RepeatRule repeatRule) {
            if (repeatRule != null) {
                this.frequency = repeatRule.getFrequency();
                this.repeatInterval = repeatRule.getRepeatInterval();
                this.byDay = repeatRule.getByDay();
                this.untilDate = repeatRule.getUntilDate();
            }
        }

        // 요일을 리스트로 접근 ("MON,TUE" -> [MON, TUE])
        public List<DayOfWeek> getByDaysList() {
            if (byDay == null || byDay.isEmpty()) {
                return List.of();
            }
            return Arrays.stream(byDay.split(","))
                    .map(String::trim)
                    .map(com.back.domain.study.plan.entity.DayOfWeek::valueOf)
                    .collect(Collectors.toList());
        }
    }
    //엔티티를 DTO로 변환하는 생성자
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