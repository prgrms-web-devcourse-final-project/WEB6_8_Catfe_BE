package com.back.domain.study.plan.service;

import com.back.domain.study.plan.dto.StudyPlanDeleteResponse;
import com.back.domain.study.plan.dto.StudyPlanRequest;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.entity.*;
import com.back.domain.study.plan.repository.StudyPlanExceptionRepository;
import com.back.domain.study.plan.repository.StudyPlanRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.back.domain.study.plan.entity.ApplyScope.THIS_ONLY;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPlanService {
    private final StudyPlanRepository studyPlanRepository;
    private final StudyPlanExceptionRepository studyPlanExceptionRepository;
    private final UserRepository userRepository;

    // ==================== 생성 ===================
    @Transactional
    public StudyPlanResponse createStudyPlan(Long userId, StudyPlanRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        // 날짜/시간 검증
        validateDateTime(request.getStartDate(), request.getEndDate());

        // 시간 겹침 검증
        validateTimeConflict(userId, null, request.getStartDate(), request.getEndDate());

        StudyPlan studyPlan = StudyPlan.create(
                user,
                request.getSubject(),
                request.getStartDate(),
                request.getEndDate(),
                request.getColor()
        );

        // 반복 규칙 설정
        if (request.getRepeatRule() != null) {
            RepeatRule repeatRule = createRepeatRule(request.getRepeatRule(), studyPlan);
            studyPlan.setRepeatRule(repeatRule);
        }

        StudyPlan savedPlan = studyPlanRepository.save(studyPlan);
        return new StudyPlanResponse(savedPlan);
    }

    private RepeatRule createRepeatRule(StudyPlanRequest.RepeatRuleRequest request, StudyPlan studyPlan) {
        // byDay 처리
        List<DayOfWeek> byDay = getByDayForWeekly(request, studyPlan.getStartDate());

        // untilDate 처리
        LocalDate untilDate = request.getUntilDate();
        if (untilDate != null) {
            validateRepeatRuleDate(studyPlan, untilDate);
        }

        return RepeatRule.create(
                request.getFrequency(),
                request.getIntervalValue() != null ? request.getIntervalValue() : 1,
                byDay,
                untilDate
        );

    }

    // ==================== 조회 ===================
    //특정 날짜 계획 조회
    public List<StudyPlanResponse> getStudyPlansForDate(Long userId, LocalDate date) {
        //원복 계획들 + 단발성 계획 조회
        List<StudyPlan> userPlans = studyPlanRepository.findByUserId(userId);
        List<StudyPlanResponse> result = new ArrayList<>();

        for (StudyPlan plan : userPlans) {
            if (plan.getRepeatRule() == null) {
                // 단발성 계획 또는 원본(시작 날짜가 타겟 날짜랑 일치)
                // 바로 추가
                if (plan.getStartDate().toLocalDate().isEqual(date)) {
                    result.add(new StudyPlanResponse(plan));
                }
            } else {
                // 반복성 계획 - 가상 계획 생성 후 추가
                StudyPlanResponse virtualPlan = createVirtualPlanForDate(plan, date);
                if (virtualPlan != null) {
                    result.add(virtualPlan);
                }
            }
        }

        return result;
    }

    // 기간별 계획 조회
    public List<StudyPlanResponse> getStudyPlansForPeriod(Long userId, LocalDate start, LocalDate end) {
        List<StudyPlan> userPlans = studyPlanRepository.findByUserId(userId);
        List<StudyPlanResponse> result = new ArrayList<>();

        LocalDate currentDate = start;
        // 날짜 범위 내에서 반복
        while (!currentDate.isAfter(end)) {
            for (StudyPlan plan : userPlans) {
                if (plan.getRepeatRule() == null) {
                    // 단발성 계획은 그대로 추가
                    if (plan.getStartDate().toLocalDate().isEqual(currentDate)) {
                        result.add(new StudyPlanResponse(plan));
                    }
                } else {
                    // 반복성 계획은 가상 계획화 후 추가
                    StudyPlanResponse virtualPlan = createVirtualPlanForDate(plan, currentDate);
                    if (virtualPlan != null) {
                        result.add(virtualPlan);
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return result.stream()
                .sorted(Comparator.comparing(StudyPlanResponse::getStartDate))
                .collect(Collectors.toList());
    }

    // 반복 계획을 위한 가상 계획 생성
    private StudyPlanResponse createVirtualPlanForDate(StudyPlan originalPlan, LocalDate targetDate) {
        RepeatRule repeatRule = originalPlan.getRepeatRule();
        LocalDate planStartDate = originalPlan.getStartDate().toLocalDate();

        // 대상 날짜가 계획 시작일 이전이면 null 반환
        if (targetDate.isBefore(planStartDate)) {
            return null;
        }

        // untilDate 확인. 방어적 검증을 위해 null 체크 한번 더
        if (repeatRule.getUntilDate() != null &&
                targetDate.isAfter(repeatRule.getUntilDate())) {
            return null;
        }

        // 반복 패턴 확인 후 타겟 날짜가 해당되는지 확인
        if (!shouldRepeatOnDate(originalPlan, targetDate)) {
            return null;
        }

        // 해당 날짜 계획의 예외 확인
        StudyPlanException exception = getEffectiveException(originalPlan.getId(), targetDate);
        if (exception != null) {
            //삭제 타입의 경우 null
            if (exception.getExceptionType() == StudyPlanException.ExceptionType.DELETED) {
                return null;
            }
            // 수정 타입의 경우 수정된 내용으로 가상 정보 생성 후 반환
            return createModifiedVirtualPlan(originalPlan, exception, targetDate);
        }

        //예외 사항 없으면 기본 가상 계획 생성
        return createBasicVirtualPlan(originalPlan, targetDate);
    }

    //해당 날짜에 반복이 되는지 확인
    private boolean shouldRepeatOnDate(StudyPlan originalPlan, LocalDate targetDate) {
        RepeatRule repeatRule = originalPlan.getRepeatRule();
        LocalDate startDate = originalPlan.getStartDate().toLocalDate();

        switch (repeatRule.getFrequency()) {
            case DAILY:
                long daysBetween = ChronoUnit.DAYS.between(startDate, targetDate);
                return daysBetween % repeatRule.getRepeatInterval() == 0;

            case WEEKLY:
                if (repeatRule.getByDay() != null && !repeatRule.getByDay().isEmpty()) {
                    // string으로 요일을 뽑아낸 뒤 enum으로 변환.
                    // 비교해서 포함되지 않으면 false
                    DayOfWeek targetDayOfWeek = DayOfWeek.valueOf(targetDate.getDayOfWeek().name().substring(0, 3));
                    if (!repeatRule.getByDay().contains(targetDayOfWeek)) {
                        return false;
                    }
                }
                long weeksBetween = ChronoUnit.WEEKS.between(startDate, targetDate);
                return weeksBetween % repeatRule.getRepeatInterval() == 0;

            case MONTHLY:
                long monthsBetween = ChronoUnit.MONTHS.between(startDate, targetDate);
                return monthsBetween % repeatRule.getRepeatInterval() == 0 &&
                        startDate.getDayOfMonth() == targetDate.getDayOfMonth();

            default:
                return false;
        }
    }

    //타켓 날짜에 적용될 예외 정보 가져오기
    private StudyPlanException getEffectiveException(Long planId, LocalDate targetDate) {
        // 해당 날짜에 직접적인 예외가 있는지 확인
        Optional<StudyPlanException> directException = studyPlanExceptionRepository
                .findByPlanIdAndDate(planId, targetDate);
        if (directException.isPresent()) {
            return directException.get();
        }

        // FROM_THIS_DATE 범위의 예외가 있는지 확인
        // exceptionDate <= targetDate 이면서 가장 최근 것
        List<StudyPlanException> scopeExceptions = studyPlanExceptionRepository
                .findByStudyPlanIdAndApplyScopeAndExceptionDateLessThanEqual(
                        planId,
                        ApplyScope.FROM_THIS_DATE,
                        targetDate
                );

        return scopeExceptions.stream()
                .max(Comparator.comparing(StudyPlanException::getExceptionDate))
                .orElse(null);
    }

    private StudyPlanResponse createModifiedVirtualPlan(StudyPlan originalPlan, StudyPlanException exception, LocalDate targetDate) {
        StudyPlanResponse response = createBasicVirtualPlan(originalPlan, targetDate);

        // 수정된 내용 적용 (dto는 setter 유지)
        if (exception.getModifiedSubject() != null) {
            response.setSubject(exception.getModifiedSubject());
        }
        if (exception.getModifiedStartDate() != null) {
            response.setStartDate(adjustTimeForDate(exception.getModifiedStartDate(), targetDate));
        }
        if (exception.getModifiedEndDate() != null) {
            response.setEndDate(adjustTimeForDate(exception.getModifiedEndDate(), targetDate));
        }
        if (exception.getModifiedColor() != null) {
            response.setColor(exception.getModifiedColor());
        }

        // 반복 규칙 수정 적용
        if (exception.getModifiedRepeatRule() != null) {
            RepeatRuleEmbeddable modifiedRule = exception.getModifiedRepeatRule();
            StudyPlanResponse.RepeatRuleResponse newRepeatRule = new StudyPlanResponse.RepeatRuleResponse();
            newRepeatRule.setFrequency(modifiedRule.getFrequency());
            newRepeatRule.setIntervalValue(modifiedRule.getRepeatInterval());
            newRepeatRule.setByDay(modifiedRule.getByDay());
            newRepeatRule.setUntilDate(modifiedRule.getUntilDate());

            response.setRepeatRule(newRepeatRule);
        }

        return response;
    }

    private StudyPlanResponse createBasicVirtualPlan(StudyPlan originalPlan, LocalDate targetDate) {
        StudyPlanResponse response = new StudyPlanResponse();
        response.setId(originalPlan.getId());
        response.setSubject(originalPlan.getSubject());
        response.setColor(originalPlan.getColor());

        // 시간은 유지하되 날짜만 변경
        response.setStartDate(adjustTimeForDate(originalPlan.getStartDate(), targetDate));
        response.setEndDate(adjustTimeForDate(originalPlan.getEndDate(), targetDate));

        if (originalPlan.getRepeatRule() != null) {
            response.setRepeatRule(new StudyPlanResponse.RepeatRuleResponse(originalPlan.getRepeatRule()));
        }

        return response;
    }

    //시간은 유지, 날짜만 변경하는 메서드
    private LocalDateTime adjustTimeForDate(LocalDateTime originalDateTime, LocalDate targetDate) {
        return LocalDateTime.of(targetDate, originalDateTime.toLocalTime());
    }

    // ==================== 수정 ===================
    private enum UpdateType {
        ORIGINAL_PLAN_UPDATE,    // 원본 계획 수정
        REPEAT_INSTANCE_CREATE,  // 새로운 예외 생성
        REPEAT_INSTANCE_UPDATE   // 기존 예외 수정
    }

    @Transactional
    public StudyPlanResponse updateStudyPlan(Long userId, Long planId, StudyPlanRequest request, ApplyScope applyScope) {
        StudyPlan originalPlan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));

        validateUserAccess(originalPlan, userId);
        // 날짜/시간 검증
        validateDateTime(request.getStartDate(), request.getEndDate());
        // 시간 겹침 검증 (원본 계획 ID 제외)
        validateTimeConflict(userId, originalPlan.getId(), request.getStartDate(), request.getEndDate());

        // 1. 단발성 계획인 경우
        if (originalPlan.getRepeatRule() == null) {
            // 반복 계획으로 변경하는 경우 -> 반복 룰 생성 후 업데이트
            if(request.getRepeatRule() != null) {
                RepeatRule repeatRule = createRepeatRule(request.getRepeatRule(), originalPlan);
                originalPlan.setRepeatRule(repeatRule);
            }
            // 그 외 변경 사항 반영
            return updateOriginalPlan(originalPlan, request);
        }

        // 2. 반복 계획인 경우 - 원본 계획과 요청 데이터 비교하여 수정 타입 판단
        UpdateType updateType = determineUpdateType(originalPlan, request, applyScope);

        switch (updateType) {
            case ORIGINAL_PLAN_UPDATE:
                // 요청에 반복 규칙이 있으면 반복 규칙 수정 후 원본 계획 수정
                if (request.getRepeatRule() != null) {
                    updateRepeatRule(originalPlan.getRepeatRule(), request.getRepeatRule(), originalPlan);
                }
                return updateOriginalPlan(originalPlan, request);

            case REPEAT_INSTANCE_CREATE:
                return createRepeatException(originalPlan, request, applyScope);

            case REPEAT_INSTANCE_UPDATE:
                return updateExistingException(originalPlan, request, applyScope);

            default:
                throw new CustomException(ErrorCode.PLAN_CANNOT_UPDATE);
        }
    }

    // 원본과 요청(가상)을 비교
    private UpdateType determineUpdateType(StudyPlan originalPlan, StudyPlanRequest request, ApplyScope applyScope) {
        LocalDate requestDate = request.getStartDate().toLocalDate();
        LocalDate originalDate = originalPlan.getStartDate().toLocalDate();

        // 1. 요청 날짜에 기존 예외가 존재하는지 먼저 조회합니다. (원본 날짜 포함)
        Optional<StudyPlanException> existingException = studyPlanExceptionRepository
                .findByPlanIdAndDate(originalPlan.getId(), requestDate);

        // 2. 요청 날짜가 원본 날짜와 같을 경우의 특별 처리
        if (requestDate.equals(originalDate)) {

            // 2-1. 원본 날짜에 이미 예외가 존재하면 -> 기존 예외 수정
            if (existingException.isPresent()) {
                return UpdateType.REPEAT_INSTANCE_UPDATE;
            }

            // 2-2. 예외가 없고 THIS_ONLY 요청이면 -> 새 예외 생성 (단일 수정)
            if (applyScope == ApplyScope.THIS_ONLY) {
                return UpdateType.REPEAT_INSTANCE_CREATE;
            }

            // 2-3. 예외가 없고 일괄 수정이면 -> 원본 수정
            return UpdateType.ORIGINAL_PLAN_UPDATE;
        }

        // 3. 요청 날짜가 원본 날짜와 다를 경우 (다른 날짜의 가상 계획 수정)
        if (existingException.isPresent()) {
            return UpdateType.REPEAT_INSTANCE_UPDATE; // 기존 예외 수정
        } else {
            return UpdateType.REPEAT_INSTANCE_CREATE; // 새 예외 생성
        }
    }

    // 원본 계획 수정
    private StudyPlanResponse updateOriginalPlan(StudyPlan originalPlan, StudyPlanRequest request) {
        // 수정 메서드 호출
        originalPlan.update(
                request.getSubject(),
                request.getStartDate(),
                request.getEndDate(),
                request.getColor()
        );
        StudyPlan savedPlan = studyPlanRepository.save(originalPlan);
        return new StudyPlanResponse(savedPlan);
    }

    // 새로운 예외 추가
    private StudyPlanResponse createRepeatException(StudyPlan originalPlan, StudyPlanRequest request, ApplyScope applyScope) {
        LocalDate exceptionDate = request.getStartDate().toLocalDate();

        // 해당 날짜에 실제로 반복 계획이 있는지 확인
        if (!shouldRepeatOnDate(originalPlan, exceptionDate)) {
            throw new CustomException(ErrorCode.PLAN_ORIGINAL_REPEAT_NOT_FOUND);
        }

        RepeatRuleEmbeddable modifiedRepeatRule = null;
        if (request.getRepeatRule() != null) {
            modifiedRepeatRule = createRepeatRuleEmbeddable(request.getRepeatRule(), request.getStartDate());
        }

        StudyPlanException exception = StudyPlanException.createModified(
                originalPlan,
                exceptionDate,
                applyScope,
                request.getSubject(),
                request.getStartDate(),
                request.getEndDate(),
                request.getColor(),
                modifiedRepeatRule
        );

        studyPlanExceptionRepository.save(exception);
        return createVirtualPlanForDate(originalPlan, exceptionDate);
    }

    // 기존 예외 수정
    private StudyPlanResponse updateExistingException(StudyPlan originalPlan, StudyPlanRequest request, ApplyScope applyScope) {
        LocalDate exceptionDate = request.getStartDate().toLocalDate();

        StudyPlanException existingException = studyPlanExceptionRepository
                .findByPlanIdAndDate(originalPlan.getId(), exceptionDate)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_EXCEPTION_NOT_FOUND));

        // 반복 계획이 변경된 사항이 있는지 확인
        // 변경 사항 존재 시 반복 변경사항 객체 생성
        RepeatRuleEmbeddable modifiedRepeatRule = null;
        if (request.getRepeatRule() != null) {
            modifiedRepeatRule = createRepeatRuleEmbeddable(request.getRepeatRule(), request.getStartDate());
        }
        // 기존 예외 정보 업데이트
        existingException.updateModifiedContent(
                request.getSubject(),
                request.getStartDate(),
                request.getEndDate(),
                request.getColor(),
                modifiedRepeatRule,
                applyScope
        );

        studyPlanExceptionRepository.save(existingException);
        return createVirtualPlanForDate(originalPlan, exceptionDate);
    }

    // 원본의 반복 룰 수정 (엔티티)
    private void updateRepeatRule(RepeatRule repeatRule, StudyPlanRequest.RepeatRuleRequest request, StudyPlan studyPlan) {
        // byDay 처리
        List<DayOfWeek> byDay = getByDayForWeekly(request, studyPlan.getStartDate());

        repeatRule.update(
                request.getFrequency(),
                request.getIntervalValue(),
                byDay,
                request.getUntilDate()
        );
    }

    // RepeatRuleEmbeddable 생성 헬퍼 메서드 (중복 코드 제거)
    private RepeatRuleEmbeddable createRepeatRuleEmbeddable(StudyPlanRequest.RepeatRuleRequest request, LocalDateTime startDate) {
        List<DayOfWeek> byDay = getByDayForWeekly(request, startDate);

        return RepeatRuleEmbeddable.create(
                request.getFrequency(),
                request.getIntervalValue(),
                byDay,
                request.getUntilDate()
        );
    }

    // ==================== 삭제 ===================
    @Transactional
    public StudyPlanDeleteResponse deleteStudyPlan(Long userId, Long planId, LocalDate selectedDate, ApplyScope applyScope) {
        StudyPlan studyPlan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));

        validateUserAccess(studyPlan, userId);

        // 삭제 전 정보 조회
        StudyPlanResponse deletedPlan;

        if (studyPlan.getRepeatRule() == null || applyScope == null) {
            // 단발성 계획
            deletedPlan = new StudyPlanResponse(studyPlan);
            studyPlanRepository.delete(studyPlan);
        } else {
            // 반복성 계획 - 가상 계획 조회
            deletedPlan = createVirtualPlanForDate(studyPlan, selectedDate);
            if (deletedPlan == null) {
                throw new CustomException(ErrorCode.PLAN_NOT_FOUND);
            }
            deleteRepeatPlan(studyPlan, selectedDate, applyScope);
        }

        return new StudyPlanDeleteResponse(deletedPlan, applyScope);
    }

    private void deleteRepeatPlan(StudyPlan studyPlan, LocalDate selectedDate, ApplyScope applyScope) {
        switch (applyScope) {
            case FROM_THIS_DATE:
                // 원본 날짜부터 삭제하는 경우 = 전체 계획 삭제
                if (selectedDate.equals(studyPlan.getStartDate().toLocalDate())) {
                    studyPlanRepository.delete(studyPlan); // CASCADE로 RepeatRule, Exception 모두 삭제
                } else {
                    /* 선택한 날짜가 원본 날짜 이후인 경우
                     * untilDate 조정만으로도 조회가 안되게 설정이 가능하기 때문에
                     * 예외를 안 만들고 untilDate 수정으로 처리
                     */
                    RepeatRule repeatRule = studyPlan.getRepeatRule();
                    LocalDate newUntilDate = selectedDate.minusDays(1);
                    repeatRule.update(null, null, null, newUntilDate);
                    studyPlanRepository.save(studyPlan);

                    // 선택한 날짜 포함 이후의 예외들은 모두 삭제
                    studyPlanExceptionRepository.deleteByStudyPlanIdAndExceptionDateGreaterThanEqual(
                            studyPlan.getId(),
                            selectedDate
                    );
                }
                break;

            case THIS_ONLY:
                // 기존 예외가 있는지 확인
                Optional<StudyPlanException> existingException = studyPlanExceptionRepository
                        .findByPlanIdAndDate(studyPlan.getId(), selectedDate);

                if (existingException.isPresent()) {
                    StudyPlanException exception = existingException.get();

                    // 기존 예외가 FROM_THIS_DATE MODIFIED인 경우
                    // 기존 예외를 다음 날로 옮기고 현재 날짜는 삭제로 변경
                    if (exception.getApplyScope() == ApplyScope.FROM_THIS_DATE
                            && exception.getExceptionType() == StudyPlanException.ExceptionType.MODIFIED) {

                        // 다음 빈 날짜로 옮기기 (다음날에 예외 존재 시 그 다음날...반복)
                        LocalDate availableDate = findNextAvailableDateForException(
                                studyPlan,
                                selectedDate.plusDays(1)
                        );

                        // 빈 날짜가 있으면 FROM_THIS_DATE 예외 생성
                        if (availableDate != null) {
                            // 기존 수정 내용 복사 (날짜는 availableDate로 조정)
                            long daysDiff = ChronoUnit.DAYS.between(selectedDate, availableDate);

                            RepeatRuleEmbeddable modifiedRule = exception.getModifiedRepeatRule();
                            LocalDateTime adjustedStart = exception.getModifiedStartDate() != null
                                    ? exception.getModifiedStartDate().plusDays(daysDiff) : null;
                            LocalDateTime adjustedEnd = exception.getModifiedEndDate() != null
                                    ? exception.getModifiedEndDate().plusDays(daysDiff) : null;

                            StudyPlanException continuedException = StudyPlanException.createModified(
                                    studyPlan,
                                    availableDate,
                                    ApplyScope.FROM_THIS_DATE,
                                    exception.getModifiedSubject(),
                                    adjustedStart,
                                    adjustedEnd,
                                    exception.getModifiedColor(),
                                    modifiedRule
                            );
                            studyPlanExceptionRepository.save(continuedException);
                        }
                    }

                    // 현재 날짜는 삭제로 변경
                    exception.changeToDeleted(THIS_ONLY);
                    studyPlanExceptionRepository.save(exception);
                } else {
                    // 새로운 예외 생성
                    StudyPlanException exception = StudyPlanException.createDeleted(
                            studyPlan, selectedDate, THIS_ONLY
                    );
                    studyPlanExceptionRepository.save(exception);
                }
                break;
        }
    }

    // ==================== 유틸 ===================
    // 인가 (작성자 일치 확인)
    private void validateUserAccess(StudyPlan studyPlan, Long userId) {
        if (!studyPlan.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
    // 시작, 종료 날짜 검증
    private void validateDateTime(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (!startDate.isBefore(endDate)) {
            throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
        }
    }
    //시간 겹침 검증 메서드 (최적화된 DB 쿼리 + 가상 인스턴스 검증 조합)
    private void validateTimeConflict(Long userId, Long planIdToExclude, LocalDateTime newStart, LocalDateTime newEnd) {
        LocalDate newPlanDate = newStart.toLocalDate();

        // 1. DB 쿼리를 통해 요청 시간과 원본 시간대가 겹칠 가능성이 있는 계획들만 로드 (최적화)
        // 기존 조회 코드를 이용하려 했으나 성능 문제로 인해 쿼리 작성.
        // 조회기능도 리펙토링 예정
        List<StudyPlan> conflictingOriginalPlans = studyPlanRepository.findByUserIdAndNotIdAndOverlapsTime(
                userId, planIdToExclude, newStart, newEnd
        );

        if (conflictingOriginalPlans.isEmpty()) {
            return;
        }

        for (StudyPlan plan : conflictingOriginalPlans) {
            if (plan.getRepeatRule() == null) {
                // 2-1. 단발성 계획 -> 쿼리에서 이미 시간 범위가 겹친다고 걸러졌지만 재확인
                if (isOverlapping(plan.getStartDate(), plan.getEndDate(), newStart, newEnd)) {
                    throw new CustomException(ErrorCode.PLAN_TIME_CONFLICT);
                }
            } else {
                // 2-2. 반복 계획 -> 기존 메서드를 사용해 요청 날짜의 가상 인스턴스를 생성하고 검사
                StudyPlanResponse virtualPlan = createVirtualPlanForDate(plan, newPlanDate);

                if (virtualPlan != null) {
                    // 가상 인스턴스가 존재하고
                    // 해당 인스턴스의 확정된 시간이 새 계획과 겹치는지 최종 확인
                    if (isOverlapping(virtualPlan.getStartDate(), virtualPlan.getEndDate(), newStart, newEnd)) {
                        throw new CustomException(ErrorCode.PLAN_TIME_CONFLICT);
                    }
                }
            }
        }
    }
    /*
     * 두 시간 범위의 겹침을 확인하는 메서드
     * 겹치는 조건: (새로운 시작 시각 < 기존 종료 시각) && (새로운 종료 시각 > 기존 시작 시각)
     * (기존 종료 시각 == 새로운 시작 시각)은 겹치지 않는 것으로 간주
     */
    private boolean isOverlapping(LocalDateTime existingStart, LocalDateTime existingEnd, LocalDateTime newStart, LocalDateTime newEnd) {
        return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
    }

    private void validateRepeatRuleDate(StudyPlan studyPlan, LocalDate untilDate) {
        LocalDate planStartDate = studyPlan.getStartDate().toLocalDate();

        // untilDate가 계획 시작일보다 이전인 경우
        if (untilDate.isBefore(planStartDate)) {
            throw new CustomException(ErrorCode.REPEAT_INVALID_UNTIL_DATE);
        }
    }
    // WEEKLY인 경우 빈 byDay 처리 메서드 (RepeatRule, RepeatRuleEmbeddable 둘 다 사용 가능)
    private List<DayOfWeek> getByDayForWeekly(StudyPlanRequest.RepeatRuleRequest request, LocalDateTime startDate) {
        if (request.getFrequency() == Frequency.WEEKLY) {
            if (request.getByDay() == null || request.getByDay().isEmpty()) {
                DayOfWeek startDay = DayOfWeek.valueOf(startDate.getDayOfWeek().name().substring(0, 3));
                return List.of(startDay);
            }
            return request.getByDay();
        }
        return new ArrayList<>();
    }

    // THIS_ONLY로 FROM_THIS_DATE 예외를 삭제할 때 다음 빈 날짜 찾기 (재귀탐색)
    private LocalDate findNextAvailableDateForException(StudyPlan studyPlan, LocalDate startDate) {
        LocalDate currentDate = startDate;
        LocalDate untilDate = studyPlan.getRepeatRule().getUntilDate();

        // untilDate가 있으면 그것을 최대 범위로, 없으면 1년
        int maxDays = untilDate != null
                ? (int) ChronoUnit.DAYS.between(startDate, untilDate) + 1
                : 365;

        int daysChecked = 0;

        while (daysChecked < maxDays) {
            // untilDate 체크
            if (untilDate != null && currentDate.isAfter(untilDate)) {
                return null;
            }

            // 해당 날짜에 반복이 발생하는지 확인
            if (shouldRepeatOnDate(studyPlan, currentDate)) {
                // 해당 날짜에 예외가 있는지 확인
                Optional<StudyPlanException> exception = studyPlanExceptionRepository
                        .findByPlanIdAndDate(studyPlan.getId(), currentDate);

                if (!exception.isPresent()) {
                    return currentDate; // 빈 날짜 찾음
                }
            }

            currentDate = currentDate.plusDays(1);
            daysChecked++;
        }

        // 1년 동안 빈 날짜를 찾지 못한 경우 예외 발생
        throw new CustomException(ErrorCode.PLAN_TOO_MANY_EXCEPTIONS);
    }

}
