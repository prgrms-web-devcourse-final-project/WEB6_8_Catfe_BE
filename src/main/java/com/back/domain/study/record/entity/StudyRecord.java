package com.back.domain.study.record.entity;

import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.todo.entity.Todo;
import com.back.domain.studyroom.entity.Room;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class StudyRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private StudyPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private int duration;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
