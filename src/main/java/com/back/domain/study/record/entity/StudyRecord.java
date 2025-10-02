package com.back.domain.study.record.entity;

import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.studyroom.entity.Room;
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
public class StudyRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private StudyPlan studyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private Long duration;

    private LocalDateTime startTime;

    @OneToMany(mappedBy = "studyRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PauseInfo> pauseInfos = new ArrayList<>();

    private LocalDateTime endTime;
}
