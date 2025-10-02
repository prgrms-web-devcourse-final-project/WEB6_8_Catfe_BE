package com.back.domain.study.record.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class StudyRecordResponseDto {
    private Long planId;
    private Long roomId;
    private LocalDateTime startTime;
    private List<PauseInfoDto> pauseInfos = new ArrayList<>();
    private LocalDateTime endTime;
    private int duration;

    @Getter
    @NoArgsConstructor
    public static class PauseInfoDto {
        private LocalDateTime pausedAt;
        private LocalDateTime restartAt;
    }
}
