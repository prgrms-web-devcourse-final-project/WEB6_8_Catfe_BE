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
    private int repeatInterval;

    //요일은 응답에 들어있는 요일을 그대로 저장 (예: "WED")
    @Column(name = "by_day")
    private String byDay;

    private LocalDate untilDate;
}
