package com.back.domain.study.plan.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepeatRuleEmbeddable {
    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    private Integer repeatInterval;
    private List<DayOfWeek> byDay = new ArrayList<>();
    private LocalDate untilDate; // LocalDateTime → LocalDate 변경
}