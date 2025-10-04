package com.back.domain.study.record.dto;

import com.back.domain.study.record.entity.PauseInfo;
import com.back.domain.study.record.entity.StudyRecord;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class StudyRecordResponseDto {
    private Long id;
    private Long planId;
    private Long roomId;
    private LocalDateTime startTime;
    private List<PauseInfoResponseDto> pauseInfos = new ArrayList<>();
    private LocalDateTime endTime;
    private Long duration;

    public static StudyRecordResponseDto from(StudyRecord studyRecord) {
        StudyRecordResponseDto dto = new StudyRecordResponseDto();
        dto.id = studyRecord.getId();
        dto.planId = studyRecord.getStudyPlan() != null ? studyRecord.getStudyPlan().getId() : null;
        dto.roomId = studyRecord.getRoom() != null ? studyRecord.getRoom().getId() : null;
        dto.startTime = studyRecord.getStartTime();
        dto.endTime = studyRecord.getEndTime();
        dto.duration = studyRecord.getDuration();
        dto.pauseInfos = studyRecord.getPauseInfos().stream()
                .map(PauseInfoResponseDto::from)
                .collect(Collectors.toList());
        return dto;
    }

    @Getter
    @NoArgsConstructor
    public static class PauseInfoResponseDto {
        private LocalDateTime pausedAt;
        private LocalDateTime restartAt;

        public static PauseInfoResponseDto from(PauseInfo pauseInfo) {
            PauseInfoResponseDto dto = new PauseInfoResponseDto();
            dto.pausedAt = pauseInfo.getPausedAt();
            dto.restartAt = pauseInfo.getRestartAt();
            return dto;
        }
    }
}
