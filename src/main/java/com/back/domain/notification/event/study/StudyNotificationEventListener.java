package com.back.domain.notification.event.study;

import com.back.domain.notification.service.NotificationService;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyNotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // 학습 기록 등록 시 - 본인에게 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleStudyRecordCreated(StudyRecordCreatedEvent event) {
        log.info("[알림] 학습 기록 등록: userId={}, duration={}초",
                event.getUserId(), event.getDuration());

        try {
            User user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // 본인에게 개인 알림
            notificationService.createPersonalNotification(
                    user,           // receiver (본인)
                    user,           // actor (본인)
                    event.getTitle(),
                    event.getContent(),
                    "/study/records/" + event.getStudyRecordId()
            );

            log.info("[알림] 학습 기록 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 학습 기록 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }

    // 일일 목표 달성 시 - 본인에게 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleDailyGoalAchieved(DailyGoalAchievedEvent event) {
        log.info("[알림] 일일 목표 달성: userId={}, date={}, 완료={}/{}",
                event.getUserId(), event.getAchievedDate(),
                event.getCompletedPlans(), event.getTotalPlans());

        try {
            User user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            notificationService.createPersonalNotification(
                    user,
                    user,
                    event.getTitle(),
                    event.getContent(),
                    "/study/plans?date=" + event.getAchievedDate()
            );

            log.info("[알림] 일일 목표 달성 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 일일 목표 달성 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }
}