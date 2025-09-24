package com.back.domain.study.plan.dto;

import com.back.domain.study.plan.entity.Color;
import com.back.domain.study.plan.entity.Frequency;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanRequest {
    private String subject;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    private Color color;

    // RepeatRule 중첩 객체
    private RepeatRuleRequest repeatRule;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepeatRuleRequest {
        private Frequency frequency;
        private Integer intervalValue;
        private String byDay; // "MON" 형태의 문자열

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private String untilDate; // "2025-12-31" 형태
    }
}
