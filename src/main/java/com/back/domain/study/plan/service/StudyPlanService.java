package com.back.domain.study.plan.service;

import com.back.domain.study.plan.dto.StudyPlanRequest;
import com.back.domain.study.plan.dto.StudyPlanResponse;
import com.back.domain.study.plan.entity.RepeatRule;
import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.plan.entity.StudyPlanException;
import com.back.domain.study.plan.repository.StudyPlanExceptionRepository;
import com.back.domain.study.plan.repository.StudyPlanRepository;
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
public class StudyPlanService{
    private final StudyPlanRepository studyPlanRepository;
    private final StudyPlanExceptionRepository studyPlanExceptionRepository;

    // ==================== 생성 ===================
    @Transactional
    public StudyPlanResponse createStudyPlan(Long userId, StudyPlanRequest request) {
        /*User user = UserRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        */
        StudyPlan studyPlan = new StudyPlan();

        //studyPlan.setUser(user);
        studyPlan.setSubject(request.getSubject());
        studyPlan.setStartDate(request.getStartDate());
        studyPlan.setEndDate(request.getEndDate());
        studyPlan.setColor(request.getColor());

        // 반복 규칙 설정
        if (request.getRepeatRule() != null) {
            StudyPlanRequest.RepeatRuleRequest repeatRuleRequest = request.getRepeatRule();
            RepeatRule repeatRule = new RepeatRule();
            repeatRule.setStudyPlan(studyPlan);
            repeatRule.setFrequency(repeatRuleRequest.getFrequency());
            repeatRule.setRepeatInterval(repeatRuleRequest.getIntervalValue() != null ? repeatRuleRequest.getIntervalValue() : 1);

            // byDay 문자열 그대로 저장
            if (repeatRuleRequest.getByDay() != null && !repeatRuleRequest.getByDay().isEmpty()) {
                repeatRule.setByDay(repeatRuleRequest.getByDay());
            }

            // untilDate 문자열을 LocalDateTime으로 변환
            if (repeatRuleRequest.getUntilDate() != null && !repeatRuleRequest.getUntilDate().isEmpty()) {
                try {
                    LocalDateTime untilDateTime = LocalDateTime.parse(repeatRuleRequest.getUntilDate() + "T23:59:59");
                    repeatRule.setUntilDate(untilDateTime);
                } catch (Exception e) {
                    // 날짜 파싱 실패 시 무시하거나 예외 처리
                }
            }

            studyPlan.setRepeatRule(repeatRule);
        }

        //추후 변수명이나 리턴 형식은 수정 예정
        StudyPlanResponse rs =new StudyPlanResponse(studyPlanRepository.save(studyPlan));
        return rs;
    }

    //특정 날짜 계획 조회
    public List<StudyPlanResponse> getStudyPlansForDate(Long userId, LocalDate date) {
        //원복 계획들 + 단발성 계획 조회
        List<StudyPlan> userPlans = studyPlanRepository.findByUserId(userId);
        List<StudyPlanResponse> result = new ArrayList<>();

        for (StudyPlan plan : userPlans) {
            // 단발성 계획인 경우
            if (plan.getRepeatRule() == null) {
                //타겟 날짜와 시작 날짜가 같으면 추가
                if (plan.getStartDate().toLocalDate().isEqual(date)) {
                    result.add(new StudyPlanResponse(plan));
                }
            } else {
                // 반복성 계획인 경우 - 가상 계획 생성
                StudyPlanResponse virtualPlan = createVirtualPlanForDate(plan, date);
                if (virtualPlan != null) {
                    result.add(virtualPlan);
                }
            }
        }

        return result;
    }

    // 기간별 계획 조회
    public List<StudyPlanResponse> getStudyPlansForPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        List<StudyPlan> userPlans = studyPlanRepository.findByUserId(userId);
        List<StudyPlanResponse> result = new ArrayList<>();

        LocalDate currentDate = startDate;
        // 날짜 범위 내에서 반복
        while (!currentDate.isAfter(endDate)) {
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
                targetDate.isAfter(repeatRule.getUntilDate().toLocalDate())) {
            return null;
        }

        // 반복 패턴 확인 후 타겟 날짜가 해당되는지 확인
        if (!shouldRepeatOnDate(originalPlan, targetDate)) {
            return null;
        }

        // 해당 날짜 계획의 예외 확인
        StudyPlanException exception = getEffectiveException(originalPlan.getId(), targetDate);
        if (exception != null) {
            // 삭제 타입은 null 반환
            if (exception.getExceptionType() == StudyPlanException.ExceptionType.DELETED) {
                return null;
            }
            // 수정된 경우 수정된 내용으로 반환
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
                // 요일 확인
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
        List<StudyPlanException> exceptions = studyPlanExceptionRepository
                .findByStudyPlanIdAndExceptionDateBetween(
                        planId,
                        targetDate.atStartOfDay(),
                        targetDate.atTime(23, 59, 59)
                );

        StudyPlan plan = studyPlanRepository.findById(planId).orElse(null);

        // 해당 날짜에 직접적인 예외가 있는지 확인
        for (StudyPlanException exception : exceptions) {
            if (plan.getStartDate().toLocalDate().isEqual(targetDate)) {
                return exception;
            }
        }

        // FROM_THIS_DATE 범위의 예외가 있는지 확인
        List<StudyPlanException> scopeExceptions = studyPlanExceptionRepository
                .findByStudyPlanIdAndApplyScopeAndExceptionDateBefore(
                        planId,
                        StudyPlanException.ApplyScope.FROM_THIS_DATE,
                        targetDate.atTime(23, 59, 59)
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
    public StudyPlanResponse updateStudyPlan(Long userId, Long planId, StudyPlanRequest request, StudyPlanException.ApplyScope applyScope) {
        StudyPlan originalPlan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

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

    // 원본과 요청을 비교
    private UpdateType determineUpdateType(StudyPlan originalPlan, StudyPlanRequest request) {
        // 1. 반복 룰 없음 -> 단발성 이므로 원본 수정
        if (originalPlan.getRepeatRule() == null) {
            return UpdateType.ORIGINAL_PLAN_UPDATE;
        }

        // 2-1. 반복 계획에서 요청 날짜가 원본 날짜와 같음 -> 원본이므로 원본 수정
        LocalDate requestDate = request.getStartDate().toLocalDate();
        LocalDate originalDate = originalPlan.getStartDate().toLocalDate();

        if (requestDate.equals(originalDate)) {
            return UpdateType.ORIGINAL_PLAN_UPDATE;
        }

        // 2-2. 반복 계획에서 다른 날짜인 경우 -> 기존 예외 확인
        Optional<StudyPlanException> existingException = studyPlanExceptionRepository
                .findByPlanIdAndDate(originalPlan.getId(), requestDate.atStartOfDay());

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
    private StudyPlanResponse createRepeatException(StudyPlan originalPlan, StudyPlanRequest request, StudyPlanException.ApplyScope applyScope) {
        LocalDate exceptionDate = request.getStartDate().toLocalDate();

        // 해당 날짜에 실제로 반복 계획이 있는지 확인
        if (!shouldRepeatOnDate(originalPlan, exceptionDate)) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        StudyPlanException exception = new StudyPlanException();
        exception.setStudyPlan(originalPlan);
        exception.setExceptionDate(exceptionDate.atStartOfDay());
        exception.setExceptionType(StudyPlanException.ExceptionType.MODIFIED);
        exception.setApplyScope(applyScope); // 파라미터로 받은 applyScope

        // 수정된 내용 설정
        if (request.getSubject() != null) exception.setModifiedSubject(request.getSubject());
        if (request.getStartDate() != null) exception.setModifiedStartDate(request.getStartDate());
        if (request.getEndDate() != null) exception.setModifiedEndDate(request.getEndDate());
        if (request.getColor() != null) exception.setModifiedColor(request.getColor());

        studyPlanExceptionRepository.save(exception);

        // 수정된 가상 계획 반환
        return createVirtualPlanForDate(originalPlan, exceptionDate);
    }

    // 기존 예외 수정
    private StudyPlanResponse updateExistingException(StudyPlan originalPlan, StudyPlanRequest request, StudyPlanException.ApplyScope applyScope) {
        LocalDate exceptionDate = request.getStartDate().toLocalDate();

        StudyPlanException existingException = studyPlanExceptionRepository
                .findByPlanIdAndDate(originalPlan.getId(), exceptionDate.atStartOfDay())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 기존 예외 정보 업데이트
        if (request.getSubject() != null) existingException.setModifiedSubject(request.getSubject());
        if (request.getStartDate() != null) existingException.setModifiedStartDate(request.getStartDate());
        if (request.getEndDate() != null) existingException.setModifiedEndDate(request.getEndDate());
        if (request.getColor() != null) existingException.setModifiedColor(request.getColor());

        // ApplyScope도 업데이트 (사용자가 범위를 변경할 수 있음)
        existingException.setApplyScope(applyScope);

        studyPlanExceptionRepository.save(existingException);

        // 수정된 가상 계획 반환
        return createVirtualPlanForDate(originalPlan, exceptionDate);
    }


    // 반복 룰 수정
    private void updateRepeatRule(RepeatRule repeatRule, StudyPlanRequest.RepeatRuleRequest request) {
        if (request.getFrequency() != null) repeatRule.setFrequency(request.getFrequency());
        if (request.getIntervalValue() != null) repeatRule.setRepeatInterval(request.getIntervalValue());
        if (request.getByDay() != null) repeatRule.setByDay(request.getByDay());

        if (request.getUntilDate() != null && !request.getUntilDate().isEmpty()) {
            try {
                LocalDateTime untilDateTime = LocalDateTime.parse(request.getUntilDate() + "T23:59:59");
                repeatRule.setUntilDate(untilDateTime);
            } catch (Exception e) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
        }
    }

    // ==================== 삭제 ===================


    // ==================== 유틸 ===================
    private void validateUserAccess(StudyPlan studyPlan, Long userId) {
        if (!studyPlan.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }




}
