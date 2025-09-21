package com.back.domain.study.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class RepeatRule extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "study_plan_id")
    private StudyPlan studyPlan;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    private int RepeatInterval;

    @Enumerated(EnumType.STRING)
    private DayOfWeek byDay;

    private LocalDateTime until;
}
