package com.back.domain.study.record.service;

import com.back.domain.notification.event.study.DailyGoalAchievedEvent;
import com.back.domain.notification.event.study.StudyRecordCreatedEvent;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.plan.repository.StudyPlanRepository;
import com.back.domain.study.plan.service.StudyPlanService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyRecordService {
    private final StudyPlanService studyPlanService;
    private final StudyRecordRepository studyRecordRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        // 방 조회 (필수)
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

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

        // 학습 기록 등록 이벤트 발행
        eventPublisher.publishEvent(
                new StudyRecordCreatedEvent(
                        userId,
                        saved.getId(),
                        saved.getStudyPlan().getId(),
                        saved.getDuration()
                )
        );

        // 일일 목표 달성 여부 체크 후 이벤트 발행
        checkAndNotifyDailyGoalAchievement(userId, saved.getStartTime().toLocalDate());

        return StudyRecordResponseDto.from(saved);
    }

    // ===================== 조회 =====================
    // 날짜별 학습 기록 조회
    public List<StudyRecordResponseDto> getStudyRecordsByDate(Long userId, LocalDate date) {
        // 유저 조회 및 권한 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 오전 0시 기준 하루의 시작과 끝 설정
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // 시작~종료 시간을 포함하는 일자의 학습 기록 조회
        List<StudyRecord> records = studyRecordRepository
                .findByUserIdAndDateRange(userId, startOfDay, endOfDay);

        return records.stream()
                .map(StudyRecordResponseDto::from)
                .collect(Collectors.toList());
    }

    // ===================== 이벤트 체크 =====================
    // 일일 목표 달성 여부 체크
    private void checkAndNotifyDailyGoalAchievement(Long userId, LocalDate date) {
        try {
            // 오늘의 학습 계획 조회
            List<StudyPlan> todayPlans = getTodayStudyPlans(userId, date);

            if (todayPlans.isEmpty()) {
                return;
            }

            // 오늘 완료한 계획 개수
            int completedCount = 0;
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            for (StudyPlan plan : todayPlans) {
                boolean hasRecord = studyRecordRepository.existsByStudyPlanIdAndDate(
                        plan.getId(),
                        startOfDay,
                        endOfDay
                );
                if (hasRecord) {
                    completedCount++;
                }
            }

            // 모든 계획 완료 시 이벤트 발행
            if (completedCount == todayPlans.size()) {
                eventPublisher.publishEvent(
                        new DailyGoalAchievedEvent(
                                userId,
                                date,
                                completedCount,
                                todayPlans.size()
                        )
                );

                log.info("일일 목표 달성 이벤트 발행 - userId: {}, date: {}", userId, date);
            }

        } catch (Exception e) {
            log.error("일일 목표 체크 실패 - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    // 오늘의 학습 계획 조회
    private List<StudyPlan> getTodayStudyPlans(Long userId, LocalDate date) {

        List<StudyPlanResponse> planResponses = studyPlanService.getStudyPlansForDate(userId, date);

        // StudyPlanResponse에서 planId 추출하여 실제 엔티티 조회
        List<Long> planIds = planResponses.stream()
                .map(StudyPlanResponse::getId)
                .distinct()
                .collect(Collectors.toList());

        return studyPlanRepository.findAllById(planIds);
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
