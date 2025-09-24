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
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan studyPlan;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Column(name = "interval_value", nullable = false)
    private int RepeatInterval;

    //필요 시 요일 지정. 여러 요일 지정 시 ,로 구분
    //현재는 요일 하나만 지정하는 형태로 구현
    @Column(name = "by_day")
    private String byDay;

    private LocalDateTime untilDate;
}
