package com.back.domain.study.plan.dto;

import com.back.domain.study.plan.entity.Color;
import com.back.domain.study.plan.entity.DayOfWeek;
import com.back.domain.study.plan.entity.Frequency;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StudyPlanRequest {
    @NotBlank
    @Size(max = 100)
    private String subject;

    @NotNull
    private LocalDateTime startDate;
    @NotNull
    private LocalDateTime endDate;

    private Color color;

    // RepeatRule 중첩 객체
    private RepeatRuleRequest repeatRule;
    // LocalDateTime을 분 단위로 자르기 위한 setter
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate != null ? startDate.truncatedTo(ChronoUnit.MINUTES) : null;
    }
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate != null ? endDate.truncatedTo(ChronoUnit.MINUTES) : null;
    }

    // 분 단위로 자른 값을 생성자에서도 설정
    public StudyPlanRequest(String subject, LocalDateTime startDate, LocalDateTime endDate,
                            Color color, RepeatRuleRequest repeatRule) {
        this.subject = subject;
        this.startDate = startDate != null ? startDate.truncatedTo(ChronoUnit.MINUTES) : null;
        this.endDate = endDate != null ? endDate.truncatedTo(ChronoUnit.MINUTES) : null;
        this.color = color;
        this.repeatRule = repeatRule;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepeatRuleRequest {
        private Frequency frequency;
        private Integer intervalValue;
        private List<DayOfWeek> byDay = new ArrayList<>();

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate untilDate; // "2025-12-31" 형태
    }
}
