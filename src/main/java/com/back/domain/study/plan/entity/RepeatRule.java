package com.back.domain.study.plan.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepeatRule extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column(name = "interval_value", nullable = false)
    private int RepeatInterval;

    //필요 시 요일 지정
    @Enumerated(EnumType.STRING)
    private String byDay;

    private LocalDateTime untilDate;
}
