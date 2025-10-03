package com.back.domain.study.record.service;

import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.plan.repository.StudyPlanRepository;
import com.back.domain.study.record.dto.StudyRecordRequestDto;
import com.back.domain.study.record.dto.StudyRecordResponseDto;
import com.back.domain.study.record.entity.PauseInfo;
import com.back.domain.study.record.entity.StudyRecord;
import com.back.domain.study.record.repository.StudyRecordRepository;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyRecordService {
    private final StudyRecordRepository studyRecordRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    // ===================== 생성 =====================
    // 학습 기록 생성 (종료 시 한 번에 기록)
    @Transactional
    public StudyRecordResponseDto createStudyRecord(Long userId, StudyRecordRequestDto request) {
        // 계획 조회
        StudyPlan studyPlan = studyPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));
        // 유저 조회 및 권한 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!studyPlan.getUser().equals(user)) {
            throw new CustomException(ErrorCode.PLAN_FORBIDDEN);
        }

        // 방 조회 (우선은 옵셔널로 설정)
        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        }

        // 일시정지 정보를 엔티티로 생성
        List<PauseInfo> pauseInfos = request.getPauseInfos().stream()
                .map(dto -> {
                    // 일시정지 시간 범위 검증
                    validateTimeRange(dto.getPausedAt(), dto.getRestartAt());
                    // 일시정지가 학습 시간 내에 있는지 검증
                    validatePauseInStudyRange(
                            request.getStartTime(),
                            request.getEndTime(),
                            dto.getPausedAt(),
                            dto.getRestartAt()
                    );
                    return PauseInfo.of(dto.getPausedAt(), dto.getRestartAt());
                })
                .collect(Collectors.toList());

        // 학습 기록 생성 (시작, 종료, 일시정지 정보 모두 포함)
        StudyRecord record = StudyRecord.create(
                user,
                studyPlan,
                room,
                request.getStartTime(),
                request.getEndTime(),
                request.getDuration(),
                pauseInfos
        );

        // 저장
        StudyRecord saved = studyRecordRepository.save(record);
        return StudyRecordResponseDto.from(saved);
    }
    // ===================== 조회 =====================


    // ===================== 유틸 =====================
    // 시간 범위 검증
    private void validateTimeRange(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
        }
    }

    // 일시정지 시간이 학습 시간 내에 있는지 검증
    private void validatePauseInStudyRange(java.time.LocalDateTime studyStart, java.time.LocalDateTime studyEnd,
                                           java.time.LocalDateTime pauseStart, java.time.LocalDateTime pauseEnd) {
        if (pauseStart.isBefore(studyStart) || pauseEnd.isAfter(studyEnd)) {
            throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
        }
    }
}
