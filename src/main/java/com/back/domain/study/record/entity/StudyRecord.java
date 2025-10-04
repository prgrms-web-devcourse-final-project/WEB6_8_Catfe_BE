package com.back.domain.study.record.entity;

import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
public class StudyRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan studyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    // 초 단위
    @Column(nullable = false)
    private Long duration;

    private LocalDateTime startTime;

    @OneToMany(mappedBy = "studyRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PauseInfo> pauseInfos = new ArrayList<>();

    private LocalDateTime endTime;

    public static StudyRecord create(User user, StudyPlan studyPlan, Room room,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     List<PauseInfo> pauseInfos) {
        StudyRecord record = new StudyRecord();
        record.user = user;
        record.studyPlan = studyPlan;
        record.room = room;
        record.startTime = startTime;
        record.endTime = endTime;

        // 일시정지 정보 추가
        if (pauseInfos != null && !pauseInfos.isEmpty()) {
            pauseInfos.forEach(record::addPauseInfo);
        }
        // 총 학습 시간 계산 (검증은 서비스에서)
        record.calculateDuration();

        return record;
    }

    // 일시정지 정보 추가
    private void addPauseInfo(PauseInfo pauseInfo) {
        this.pauseInfos.add(pauseInfo);
        pauseInfo.assignStudyRecord(this);
    }

    // 실제 학습 시간 계산 (전체 시간 - 일시정지 시간)
    private void calculateDuration() {
        // 전체 시간 계산 (초)
        long totalSeconds = Duration.between(startTime, endTime).getSeconds();

        // 일시정지 시간 제외
        long pausedSeconds = pauseInfos.stream()
                .filter(pause -> pause.getPausedAt() != null && pause.getRestartAt() != null)
                .mapToLong(pause -> Duration.between(pause.getPausedAt(), pause.getRestartAt()).getSeconds())
                .sum();

        this.duration = totalSeconds - pausedSeconds;;
    }
}
