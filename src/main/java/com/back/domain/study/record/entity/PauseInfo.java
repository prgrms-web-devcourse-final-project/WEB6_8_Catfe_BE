package com.back.domain.study.record.entity;


import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "pause_infos")
public class PauseInfo {
    // 일시정지 정보에 생성, 수정일은 필요 없을 것 같아서
    // id만 별도로 생성
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_record_id", nullable = false)
    private StudyRecord studyRecord;

    @Column(name = "paused_at", nullable = false)
    private LocalDateTime pausedAt;

    @Column(name = "restart_at")
    private LocalDateTime restartAt;

    public static PauseInfo of(LocalDateTime pausedAt, LocalDateTime restartAt) {
        PauseInfo pauseInfo = new PauseInfo();
        pauseInfo.pausedAt = pausedAt;
        pauseInfo.restartAt = restartAt;
        return pauseInfo;
    }

    // 양방향 연관관계 설정
    void assignStudyRecord(StudyRecord studyRecord) {
        this.studyRecord = studyRecord;
    }

}
