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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
                    LocalDateTime pausedAt = dto.getPausedAt();
                    LocalDateTime restartAt = dto.getRestartAt();

                    // 재시작 안 했으면 학습 종료 시간을 재시작 시간으로 간주
                    if (restartAt == null) {
                        restartAt = request.getEndTime();
                    }

                    // 일시정지 시간 범위 검증
                    validateTimeRange(pausedAt, restartAt);

                    // 일시정지가 학습 시간 내에 있는지 검증
                    validatePauseInStudyRange(
                            request.getStartTime(),
                            request.getEndTime(),
                            pausedAt,
                            restartAt
                    );
                    return PauseInfo.of(pausedAt, restartAt);
                })
                .collect(Collectors.toList());

        // 학습 기록 생성 (시작, 종료, 일시정지 정보 모두 포함)
        StudyRecord record = StudyRecord.create(
                user,
                studyPlan,
                room,
                request.getStartTime(),
                request.getEndTime(),
                pauseInfos
        );

        // 프론트 Duration과 백엔드 Duration 비교 검증
        validateDurationDifference(request.getDuration(), record.getDuration());

        // 저장
        StudyRecord saved = studyRecordRepository.save(record);
        return StudyRecordResponseDto.from(saved);
    }
    // ===================== 조회 =====================
    // 날짜별 학습 기록 조회
    public List<StudyRecordResponseDto> getStudyRecordsByDate(Long userId, LocalDate date) {
        // 유저 조회 및 권한 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 오전 4시 기준 하루의 시작과 끝 설정
        LocalDateTime startOfDay = date.atTime(4, 0, 0);
        LocalDateTime endOfDay = date.plusDays(1).atTime(4, 0, 0);

        // 시작~종료 시간을 포함하는 일자의 학습 기록 조회
        List<StudyRecord> records = studyRecordRepository
                .findByUserIdAndDateRange(userId, startOfDay, endOfDay);

        return records.stream()
                .map(StudyRecordResponseDto::from)
                .collect(Collectors.toList());
    }

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
    // 프론트에서 계산한 학습 시간과 백엔드에서 계산한 학습 시간의 차이가 5초 이상이면 예외 발생
    private void validateDurationDifference(Long frontDuration, Long backendDuration) {
        long difference = Math.abs(frontDuration - backendDuration);

        // 5초 이상 차이나면 예외 발생
        if (difference > 5) {
            throw new CustomException(ErrorCode.DURATION_MISMATCH);
        }
    }
}
