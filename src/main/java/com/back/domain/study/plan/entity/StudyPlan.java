package com.back.domain.study.plan.entity;

import com.back.domain.study.record.entity.StudyRecord;
import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class StudyPlan extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String subject;


    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private Color color;


    @OneToOne(mappedBy = "studyPlan",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RepeatRule repeatRule;

    @OneToMany(mappedBy = "studyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyRecord> studyRecords;

    //반복 주기 설정 시 예외 리스트
    @OneToMany(mappedBy = "studyPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StudyPlanException> exceptions = new ArrayList<>();

    // 정적 팩토리 메서드
    public static StudyPlan create(User user, String subject, LocalDateTime startDate,
                                   LocalDateTime endDate, Color color) {
        StudyPlan plan = new StudyPlan();
        plan.user = user;
        plan.subject = subject;
        plan.startDate = startDate;
        plan.endDate = endDate;
        plan.color = color;
        return plan;
    }

    // 수정 메서드
    public void update(String subject, LocalDateTime startDate, LocalDateTime endDate, Color color) {
        if (subject != null) this.subject = subject;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (color != null) this.color = color;
    }
    // 반복 규칙 설정 메서드
    public void setRepeatRule(RepeatRule repeatRule) {
        this.repeatRule = repeatRule;
        if (repeatRule != null) {
            repeatRule.setStudyPlan(this);
        }
    }
}
