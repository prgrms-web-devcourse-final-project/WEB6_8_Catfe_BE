package com.back.domain.study.plan.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private List<DayOfWeek> byDay = new ArrayList<>();

    private LocalDate untilDate;

    // 정적 팩토리 메서드
    public static RepeatRule create(Frequency frequency, int repeatInterval,
                                    List<DayOfWeek> byDay, LocalDate untilDate) {
        RepeatRule rule = new RepeatRule();
        rule.frequency = frequency;
        rule.repeatInterval = repeatInterval;
        rule.byDay = byDay != null ? byDay : new ArrayList<>();
        rule.untilDate = untilDate;
        return rule;
    }

    // 수정 메서드
    public void update(Frequency frequency, Integer repeatInterval,
                       List<DayOfWeek> byDay, LocalDate untilDate) {
        if (frequency != null) this.frequency = frequency;
        if (repeatInterval != null) this.repeatInterval = repeatInterval;
        if (byDay != null) this.byDay = byDay;
        if (untilDate != null) this.untilDate = untilDate;
    }

    // 계획 연결 메서드
    public void setStudyPlan(StudyPlan studyPlan) {
        this.studyPlan = studyPlan;
    }
}
