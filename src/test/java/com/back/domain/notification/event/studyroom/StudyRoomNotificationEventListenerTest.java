package com.back.domain.notification.event.studyroom;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyRoomNotificationEventListener 테스트")
class StudyRoomNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StudyRoomNotificationEventListener listener;

    @Test
    @DisplayName("스터디룸 공지사항 생성 이벤트")
    void t1() {
        // given
        StudyRoomNoticeCreatedEvent event = new StudyRoomNoticeCreatedEvent(1L, 10L, "공지 제목", "내용");

        // when
        listener.handleNoticeCreated(event);

        // then
        verify(notificationService).createRoomNotification(
                eq(10L), // roomId
                eq(1L),  // actorId
                anyString(),
                anyString(),
                anyString(),
                eq(NotificationSettingType.ROOM_NOTICE)
        );
    }

    @Test
    @DisplayName("멤버 권한 변경 이벤트")
    void t2() {
        // given
        MemberRoleChangedEvent event = new MemberRoleChangedEvent(1L, 100L, 2L, "MANAGER");

        // when
        listener.handleMemberRoleChanged(event);

        // then
        verify(notificationService).createPersonalNotification(
                eq(2L), // receiverId (targetUserId)
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/rooms/100"),
                eq(NotificationSettingType.ROOM_JOIN)
        );
    }

    @Test
    @DisplayName("멤버 추방 이벤트")
    void t3() {
        // given
        MemberKickedEvent event = new MemberKickedEvent(1L, 100L, 2L, "테스트 방");

        // when
        listener.handleMemberKicked(event);

        // then
        verify(notificationService).createPersonalNotification(
                eq(2L), // receiverId (targetUserId)
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/rooms"),
                eq(NotificationSettingType.ROOM_JOIN)
        );
    }

    @Test
    @DisplayName("방장 위임 이벤트")
    void t4() {
        // given
        OwnerTransferredEvent event = new OwnerTransferredEvent(1L, 100L, 2L, "테스트 방");

        // when
        listener.handleOwnerTransferred(event);

        // then
        verify(notificationService).createPersonalNotification(
                eq(2L), // receiverId (newOwnerId)
                eq(1L), // actorId
                anyString(),
                anyString(),
                eq("/rooms/100"),
                eq(NotificationSettingType.ROOM_JOIN)
        );
    }

    @Test
    @DisplayName("알림 생성 중 예외 발생 - 로그만 출력하고 예외 전파 안함")
    void t5() {
        // given
        MemberRoleChangedEvent event = new MemberRoleChangedEvent(1L, 100L, 2L, "MANAGER");
        willThrow(new RuntimeException("DB 오류"))
                .given(notificationService)
                .createPersonalNotification(anyLong(), anyLong(), any(), any(), any(), any());

        // when & then
        assertThatCode(() -> listener.handleMemberRoleChanged(event))
                .doesNotThrowAnyException();

        verify(notificationService).createPersonalNotification(anyLong(), anyLong(), any(), any(), any(), any());
    }
}