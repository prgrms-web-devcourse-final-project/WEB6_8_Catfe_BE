package com.back.domain.notification.event.study;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
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

    // 학습 기록 등록 시 - 본인에게 알림
    @EventListener
    @Async("notificationExecutor")
    public void handleStudyRecordCreated(StudyRecordCreatedEvent event) {
        log.info("[알림] 학습 기록 등록: userId={}, duration={}초",
                event.getUserId(), event.getDuration());

        try {
            notificationService.createSelfNotification(
                    event.getUserId(),
                    event.getTitle(),
                    event.getContent(),
                    "/study/records/" + event.getStudyRecordId(),
                    NotificationSettingType.SYSTEM
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
            notificationService.createSelfNotification(
                    event.getUserId(),
                    event.getTitle(),
                    event.getContent(),
                    "/study/plans?date=" + event.getAchievedDate(),
                    NotificationSettingType.SYSTEM
            );

            log.info("[알림] 일일 목표 달성 알림 전송 완료");

        } catch (Exception e) {
            log.error("[알림] 일일 목표 달성 알림 전송 실패: error={}", e.getMessage(), e);
        }
    }
}