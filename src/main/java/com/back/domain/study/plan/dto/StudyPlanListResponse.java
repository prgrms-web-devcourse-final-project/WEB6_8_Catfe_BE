package com.back.domain.study.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanListResponse {
    private LocalDate date;
    private List<StudyPlanResponse> plans;
    private int totalCount;
}
