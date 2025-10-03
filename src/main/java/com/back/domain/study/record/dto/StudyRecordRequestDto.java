package com.back.domain.study.record.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class StudyRecordRequestDto {
    private Long planId;
    private Long roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private List<PauseInfoRequestDto> pauseInfos = new ArrayList<>();

    @Getter
    @NoArgsConstructor
    public static class PauseInfoRequestDto {
        private LocalDateTime pausedAt;
        private LocalDateTime restartAt;
    }
}
