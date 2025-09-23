package com.back.domain.study.plan.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
public class StudyPlan extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String subject;

    @Enumerated(EnumType.STRING)
    private StudyStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private Color color;

    // 부모 계획과의 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_plan_id",
            foreignKey = @ForeignKey(name = "fk_study_plan_parent"))
    private StudyPlan parentPlan;

    // 자식 계획들과 연관관계
    @OneToMany(mappedBy = "parentPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StudyPlan> childPlans = new ArrayList<>();

    @OneToOne(mappedBy = "studyPlan",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RepeatRule repeatRule;
}
