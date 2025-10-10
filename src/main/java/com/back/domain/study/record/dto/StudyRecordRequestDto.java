package com.back.domain.study.record.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class StudyRecordRequestDto {
    @NotNull(message = "계획 ID는 필수입니다.")
    private Long planId;
    @NotNull(message = "방 ID는 필수입니다.")
    private Long roomId;
    @NotNull(message = "시작 시간은 필수입니다.")
    private LocalDateTime startTime;
    @NotNull(message = "종료 시간은 필수입니다.")
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
