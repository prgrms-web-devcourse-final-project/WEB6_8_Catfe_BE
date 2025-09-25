package com.back.domain.study.plan.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepeatRule extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan studyPlan;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column(name = "interval_value", nullable = false)
    private int RepeatInterval;

    //요일은 계획 날짜에 따라 자동 설정
    @Column(name = "by_day")
    private String byDay;

    private LocalDate untilDate;
}
