package com.back.domain.study.plan.dto;

import com.back.domain.study.plan.entity.ApplyScope;
import com.back.domain.study.plan.entity.Color;
import com.back.domain.study.plan.entity.StudyPlan;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudyPlanDeleteResponse {
    private Long id;
    private String subject;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Color color;
    private LocalDate deletedDate;  // 삭제된 날짜
    private ApplyScope applyScope;  // 삭제 범위

    public StudyPlanDeleteResponse(StudyPlanResponse plan, ApplyScope applyScope) {
        this.id = plan.getId();
        this.subject = plan.getSubject();
        this.startDate = plan.getStartDate();
        this.endDate = plan.getEndDate();
        this.color = plan.getColor();
        this.deletedDate = plan.getStartDate().toLocalDate();
        this.applyScope = applyScope;
    }
}
