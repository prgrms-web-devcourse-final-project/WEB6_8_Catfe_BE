package com.back.domain.study.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class StudyPlan extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String subject;

    @Enumerated(EnumType.STRING)
    private StudyStatus status;

    private LocalDateTime studyDate;

    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private Color color;

    @Enumerated(EnumType.STRING)
    private RepeatType repeatType;

    @OneToOne(mappedBy = "studyPlan")
    private RepeatRule repeatRule;
}
