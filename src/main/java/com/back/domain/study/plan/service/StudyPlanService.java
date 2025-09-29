package com.back.domain.study.plan.service;

import com.back.domain.study.plan.dto.StudyPlanDeleteRequest;
import com.back.domain.study.plan.dto.StudyPlanRequest;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.entity.*;
import com.back.domain.study.plan.repository.StudyPlanExceptionRepository;
import com.back.domain.study.plan.repository.StudyPlanRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
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


        StudyPlan studyPlan = new StudyPlan();

        studyPlan.setUser(user);
        studyPlan.setSubject(request.getSubject());
        studyPlan.setStartDate(request.getStartDate());
        studyPlan.setEndDate(request.getEndDate());
        studyPlan.setColor(request.getColor());

        // 반복 규칙 설정
        if (request.getRepeatRule() != null) {
            RepeatRule repeatRule = createRepeatRule(request.getRepeatRule(), studyPlan);
            studyPlan.setRepeatRule(repeatRule);
        }

        StudyPlan savedPlan = studyPlanRepository.save(studyPlan);
        return new StudyPlanResponse(savedPlan);
    }

    private RepeatRule createRepeatRule(StudyPlanRequest.RepeatRuleRequest request, StudyPlan studyPlan) {
        RepeatRule repeatRule = new RepeatRule();
        repeatRule.setStudyPlan(studyPlan);
        repeatRule.setFrequency(request.getFrequency());
        repeatRule.setRepeatInterval(request.getIntervalValue() != null ? request.getIntervalValue() : 1);

        if (request.getByDay() != null && !request.getByDay().isEmpty()) {
            repeatRule.setByDay(request.getByDay());
        }
        // untilDate 설정 및 검증
        if (request.getUntilDate() != null && !request.getUntilDate().isEmpty()) {
            LocalDate untilDate = LocalDate.parse(request.getUntilDate());
            // 형식에 안맞는 경우
            try {
                repeatRule.setUntilDate(untilDate);
            } catch (Exception e) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
            // untilDate가 시작일 이전인지 검증
            validateRepeatRuleDate(studyPlan, untilDate);
        }

        return repeatRule;
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
                    String targetDayOfWeek = targetDate.getDayOfWeek().name().substring(0, 3);
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
        List<StudyPlanException> scopeExceptions = studyPlanExceptionRepository
                .findByStudyPlanIdAndApplyScopeAndExceptionDateBefore(
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

        // 수정된 내용 적용
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
            newRepeatRule.setRepeatInterval(modifiedRule.getIntervalValue());
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

        // 1. 단발성 계획인 경우 applyScope 무시하고 원본 수정
        if (originalPlan.getRepeatRule() == null) {
            return updateOriginalPlan(originalPlan, request);
        }

        // 2. 반복 계획인 경우 - 원본 계획과 요청 데이터 비교하여 수정 타입 판단
        UpdateType updateType = determineUpdateType(originalPlan, request);

        switch (updateType) {
            case ORIGINAL_PLAN_UPDATE:
                return updateOriginalPlan(originalPlan, request);

            case REPEAT_INSTANCE_CREATE:
                return createRepeatException(originalPlan, request, applyScope);

            case REPEAT_INSTANCE_UPDATE:
                return updateExistingException(originalPlan, request, applyScope);

            default:
                throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    // 원본과 요청(가상)을 비교
    private UpdateType determineUpdateType(StudyPlan originalPlan, StudyPlanRequest request) {
        LocalDate requestDate = request.getStartDate().toLocalDate();
        LocalDate originalDate = originalPlan.getStartDate().toLocalDate();

        // 1-1. 반복 계획에서 요청 날짜가 원본 날짜와 같음 -> 원본이므로 원본 수정
        if (requestDate.equals(originalDate)) {
            return UpdateType.ORIGINAL_PLAN_UPDATE;
        }

        // 1-2. 반복 계획에서 다른 날짜인 경우 -> 기존 예외 존재 유무 확인
        Optional<StudyPlanException> existingException = studyPlanExceptionRepository
                .findByPlanIdAndDate(originalPlan.getId(), requestDate);

        if (existingException.isPresent()) {
            return UpdateType.REPEAT_INSTANCE_UPDATE; // 기존 예외 수정
        } else {
            return UpdateType.REPEAT_INSTANCE_CREATE; // 새 예외 생성
        }
    }

    // 원본 계획 수정
    private StudyPlanResponse updateOriginalPlan(StudyPlan originalPlan, StudyPlanRequest request) {
        // 원본 계획 직접 수정
        if (request.getSubject() != null) originalPlan.setSubject(request.getSubject());
        if (request.getStartDate() != null) originalPlan.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) originalPlan.setEndDate(request.getEndDate());
        if (request.getColor() != null) originalPlan.setColor(request.getColor());

        // 반복 규칙 수정
        if (request.getRepeatRule() != null && originalPlan.getRepeatRule() != null) {
            updateRepeatRule(originalPlan.getRepeatRule(), request.getRepeatRule());
        }

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

        StudyPlanException exception = new StudyPlanException();
        exception.setStudyPlan(originalPlan);
        exception.setExceptionDate(exceptionDate);
        exception.setExceptionType(StudyPlanException.ExceptionType.MODIFIED);
        exception.setApplyScope(applyScope); // 파라미터로 받은 applyScope

        // 수정된 내용 설정
        if (request.getSubject() != null) exception.setModifiedSubject(request.getSubject());
        if (request.getStartDate() != null) exception.setModifiedStartDate(request.getStartDate());
        if (request.getEndDate() != null) exception.setModifiedEndDate(request.getEndDate());
        if (request.getColor() != null) exception.setModifiedColor(request.getColor());

        // 반복 규칙 수정. 요청에 반복 규칙이 있으면 설정
        if (request.getRepeatRule() != null) {
            RepeatRuleEmbeddable embeddable = new RepeatRuleEmbeddable();
            embeddable.setFrequency(request.getRepeatRule().getFrequency());
            embeddable.setIntervalValue(request.getRepeatRule().getIntervalValue());
            embeddable.setByDay(request.getRepeatRule().getByDay());

            if (request.getRepeatRule().getUntilDate() != null && !request.getRepeatRule().getUntilDate().isEmpty()) {
                try {
                    LocalDate untilDate = LocalDate.parse(request.getRepeatRule().getUntilDate());
                    embeddable.setUntilDate(untilDate);
                } catch (Exception e) {
                    throw new CustomException(ErrorCode.PLAN_INVALID_DATE_FORMAT);
                }
            }

            exception.setModifiedRepeatRule(embeddable);
        }


        studyPlanExceptionRepository.save(exception);
        return createVirtualPlanForDate(originalPlan, exceptionDate);
    }

    // 기존 예외 수정
    private StudyPlanResponse updateExistingException(StudyPlan originalPlan, StudyPlanRequest request, ApplyScope applyScope) {
        LocalDate exceptionDate = request.getStartDate().toLocalDate();

        StudyPlanException existingException = studyPlanExceptionRepository
                .findByPlanIdAndDate(originalPlan.getId(), exceptionDate)
                .orElse(null);

        // 기존 예외 정보 업데이트
        if (request.getSubject() != null) existingException.setModifiedSubject(request.getSubject());
        if (request.getStartDate() != null) existingException.setModifiedStartDate(request.getStartDate());
        if (request.getEndDate() != null) existingException.setModifiedEndDate(request.getEndDate());
        if (request.getColor() != null) existingException.setModifiedColor(request.getColor());

        // ApplyScope도 업데이트 (사용자가 범위를 변경할 수 있음)
        existingException.setApplyScope(applyScope);

        // 반복 규칙 수정사항 있으면 예외 안에 추가 (embeddable)
        if (request.getRepeatRule() != null) {
            RepeatRuleEmbeddable embeddable = new RepeatRuleEmbeddable();
            embeddable.setFrequency(request.getRepeatRule().getFrequency());
            embeddable.setIntervalValue(request.getRepeatRule().getIntervalValue());
            embeddable.setByDay(request.getRepeatRule().getByDay());

            if (request.getRepeatRule().getUntilDate() != null && !request.getRepeatRule().getUntilDate().isEmpty()) {
                try {
                    LocalDate untilDate = LocalDate.parse(request.getRepeatRule().getUntilDate());
                    embeddable.setUntilDate(untilDate);
                } catch (Exception e) {
                    throw new CustomException(ErrorCode.PLAN_INVALID_DATE_FORMAT);
                }
            }

            existingException.setModifiedRepeatRule(embeddable);
        }

        studyPlanExceptionRepository.save(existingException);
        return createVirtualPlanForDate(originalPlan, exceptionDate);
    }


    // 원본의 반복 룰 수정 (엔티티)
    private void updateRepeatRule(RepeatRule repeatRule, StudyPlanRequest.RepeatRuleRequest request) {
        if (request.getFrequency() != null) repeatRule.setFrequency(request.getFrequency());
        if (request.getIntervalValue() != null) repeatRule.setRepeatInterval(request.getIntervalValue());
        if (request.getByDay() != null) repeatRule.setByDay(request.getByDay());

        if (request.getUntilDate() != null && !request.getUntilDate().isEmpty()) {
            try {
                LocalDate untilDate = LocalDate.parse(request.getUntilDate());
                repeatRule.setUntilDate(untilDate);
            } catch (Exception e) {
                throw new CustomException(ErrorCode.PLAN_INVALID_DATE_FORMAT);
            }
        }
    }

    // ==================== 삭제 ===================
    @Transactional
    public void deleteStudyPlan(Long userId, Long planId, LocalDate selectedDate, ApplyScope applyScope) {
        StudyPlan studyPlan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));

        validateUserAccess(studyPlan, userId);

        // 단발성 계획 삭제 (반복 룰이 null이거나 applyScope가 null인 경우)
        if (studyPlan.getRepeatRule() == null || applyScope == null) {
            studyPlanRepository.delete(studyPlan);
            return;
        }

        // 반복성 계획 삭제 - applyScope에 따른 처리
        deleteRepeatPlan(studyPlan, selectedDate, applyScope);
    }

    private void deleteRepeatPlan(StudyPlan studyPlan, LocalDate selectedDate, ApplyScope applyScope) {
        switch (applyScope) {
            case FROM_THIS_DATE:
                // 원본 날짜부터 삭제하는 경우 = 전체 계획 삭제
                if (selectedDate.equals(studyPlan.getStartDate().toLocalDate())) {
                    studyPlanRepository.delete(studyPlan); // CASCADE로 RepeatRule, Exception 모두 삭제
                } else {
                    // 중간 날짜부터 삭제하는 경우 = untilDate 수정
                    RepeatRule repeatRule = studyPlan.getRepeatRule();
                    LocalDate newUntilDate = selectedDate.minusDays(1);
                    repeatRule.setUntilDate(newUntilDate);
                    studyPlanRepository.save(studyPlan);
                }
                break;

            case THIS_ONLY:
                // 선택한 날짜만 삭제 - 예외 생성
                StudyPlanException exception = new StudyPlanException();
                exception.setStudyPlan(studyPlan);
                exception.setExceptionDate(selectedDate);
                exception.setExceptionType(StudyPlanException.ExceptionType.DELETED);
                exception.setApplyScope(ApplyScope.THIS_ONLY);
                studyPlanExceptionRepository.save(exception);
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

    private void validateDateTime(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (!startDate.isBefore(endDate)) {
            throw new CustomException(ErrorCode.PLAN_INVALID_TIME_RANGE);
        }
    }

    private void validateRepeatRuleDate(StudyPlan studyPlan, LocalDate untilDate) {
        LocalDate planStartDate = studyPlan.getStartDate().toLocalDate();

        // untilDate가 계획 시작일보다 이전인 경우
        if (untilDate.isBefore(planStartDate)) {
            throw new CustomException(ErrorCode.REPEAT_INVALID_UNTIL_DATE);
        }
    }

}
