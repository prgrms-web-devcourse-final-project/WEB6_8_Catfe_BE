package com.back.domain.notification.event.study;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyNotificationEventListener 테스트")
class StudyNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StudyNotificationEventListener listener;

    @Test
    @DisplayName("학습 기록 생성 이벤트")
    void t1() {
        // given
        StudyRecordCreatedEvent event = new StudyRecordCreatedEvent(1L, 100L, 200L, 3600L);

        // when
        listener.handleStudyRecordCreated(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(1L), // userId
                anyString(),
                anyString(),
                eq("/study/records/100"),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    @Test
    @DisplayName("일일 목표 달성 이벤트")
    void t2() {
        // given
        LocalDate today = LocalDate.now();
        DailyGoalAchievedEvent event = new DailyGoalAchievedEvent(1L, today, 5, 5);

        // when
        listener.handleDailyGoalAchieved(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(1L), // userId
                anyString(),
                anyString(),
                eq("/study/plans?date=" + today),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    @Test
    @DisplayName("알림 생성 중 예외 발생 - 로그만 출력하고 예외 전파 안함")
    void t3() {
        // given
        StudyRecordCreatedEvent event = new StudyRecordCreatedEvent(1L, 100L, 200L, 3600L);
        willThrow(new RuntimeException("DB 오류"))
                .given(notificationService)
                .createSelfNotification(anyLong(), anyString(), anyString(), anyString(), any());

        // when & then
        assertThatCode(() -> listener.handleStudyRecordCreated(event))
                .doesNotThrowAnyException();

        // 서비스 메서드가 호출된 것 자체는 검증
        verify(notificationService).createSelfNotification(anyLong(), anyString(), anyString(), anyString(), any());
    }
}