package com.back.domain.study.plan.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_plan_exception")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//이후 날짜 반복 계획 모두 삭제는 원본 엔티티의 untilDate를 수정해 구현
//단일 삭제 또는 단일, 이후 모두 수정은 이 엔티티로 구현
public class StudyPlanException extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;

    // 예외가 발생한 날짜
    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    //예외 유형 (수정 / 삭제)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExceptionType exceptionType;

    // 적용 범위 (이 날짜만 / 이후 모든 날짜)
    @Enumerated(EnumType.STRING)
    @Column(name = "apply_scope")
    private ApplyScope applyScope;

    // 수정된 내용 (MODIFIED 타입인 경우)
    @Column(name = "modified_subject")
    private String modifiedSubject;

    @Column(name = "modified_start_date")
    private LocalDateTime modifiedStartDate;

    @Column(name = "modified_end_date")
    private LocalDateTime modifiedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "modified_color")
    private Color modifiedColor;

    public enum ExceptionType {
        DELETED,    // 해당 날짜 삭제
        MODIFIED    // 해당 날짜 수정
    }

    @Embedded
    @Column(name = "modified_repeat_rule")
    @AttributeOverrides({
            @AttributeOverride(name = "frequency", column = @Column(name = "modified_frequency")),
            @AttributeOverride(name = "intervalValue", column = @Column(name = "modified_repeat_interval")),
            @AttributeOverride(name = "byDay", column = @Column(name = "modified_by_day")),
            @AttributeOverride(name = "untilDate", column = @Column(name = "modified_until_date"))
    })
    private RepeatRuleEmbeddable modifiedRepeatRule;
}