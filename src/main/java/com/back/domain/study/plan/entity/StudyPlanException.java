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
            @AttributeOverride(name = "repeatInterval", column = @Column(name = "modified_interval_value")),
            @AttributeOverride(name = "byDay", column = @Column(name = "modified_by_day")),
            @AttributeOverride(name = "untilDate", column = @Column(name = "modified_until_date"))
    })
    private RepeatRuleEmbeddable modifiedRepeatRule;

    // 정적 팩토리 메서드 - 수정 예외
    public static StudyPlanException createModified(StudyPlan studyPlan, LocalDate exceptionDate,
                                                    ApplyScope applyScope, String modifiedSubject,
                                                    LocalDateTime modifiedStartDate, LocalDateTime modifiedEndDate,
                                                    Color modifiedColor, RepeatRuleEmbeddable modifiedRepeatRule) {
        StudyPlanException exception = new StudyPlanException();
        exception.studyPlan = studyPlan;
        exception.exceptionDate = exceptionDate;
        exception.exceptionType = ExceptionType.MODIFIED;
        exception.applyScope = applyScope;
        exception.modifiedSubject = modifiedSubject;
        exception.modifiedStartDate = modifiedStartDate;
        exception.modifiedEndDate = modifiedEndDate;
        exception.modifiedColor = modifiedColor;
        exception.modifiedRepeatRule = modifiedRepeatRule;
        return exception;
    }

    // 정적 팩토리 메서드 - 삭제 예외
    public static StudyPlanException createDeleted(StudyPlan studyPlan, LocalDate exceptionDate,
                                                   ApplyScope applyScope) {
        StudyPlanException exception = new StudyPlanException();
        exception.studyPlan = studyPlan;
        exception.exceptionDate = exceptionDate;
        exception.exceptionType = ExceptionType.DELETED;
        exception.applyScope = applyScope;
        return exception;
    }

    // 수정 내용 업데이트
    public void updateModifiedContent(String subject, LocalDateTime startDate,
                                      LocalDateTime endDate, Color color,
                                      RepeatRuleEmbeddable repeatRule, ApplyScope applyScope) {
        if (subject != null) this.modifiedSubject = subject;
        if (startDate != null) this.modifiedStartDate = startDate;
        if (endDate != null) this.modifiedEndDate = endDate;
        if (color != null) this.modifiedColor = color;
        if (repeatRule != null) this.modifiedRepeatRule = repeatRule;
        if (applyScope != null) this.applyScope = applyScope;
    }

    // 삭제 타입으로 변경
    public void changeToDeleted(ApplyScope applyScope) {
        this.exceptionType = ExceptionType.DELETED;
        this.applyScope = applyScope;
        this.modifiedSubject = null;
        this.modifiedStartDate = null;
        this.modifiedEndDate = null;
        this.modifiedColor = null;
        this.modifiedRepeatRule = null;
    }
}