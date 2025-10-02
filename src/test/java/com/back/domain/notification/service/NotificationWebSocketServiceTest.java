package com.back.domain.notification.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

import com.back.domain.notification.dto.NotificationWebSocketDto;
import com.back.domain.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class NotificationWebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationWebSocketService webSocketService;

    private NotificationWebSocketDto notificationDto;

    @BeforeEach
    void setUp() {
        notificationDto = NotificationWebSocketDto.from(
                1L,
                "테스트 알림",
                "알림 내용",
                NotificationType.PERSONAL,
                "/target",
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("개인 알림 전송 테스트")
    class SendNotificationToUserTest {

        @Test
        @DisplayName("특정 유저에게 알림 전송")
        void t1() {
            // given
            Long userId = 1L;
            String expectedDestination = "/topic/user/1/notifications";

            // when
            webSocketService.sendNotificationToUser(userId, notificationDto);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq(expectedDestination),
                    eq(notificationDto)
            );
        }

        @Test
        @DisplayName("전송 실패 시 예외를 로깅하고 정상 종료")
        void t2() {
            // given
            Long userId = 1L;
            willThrow(new RuntimeException("전송 실패"))
                    .given(messagingTemplate)
                    .convertAndSend(anyString(), any(Object.class));

            // when & then - 예외가 발생해도 메서드는 정상 종료되어야 함
            webSocketService.sendNotificationToUser(userId, notificationDto);

            verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("시스템 알림 브로드캐스트 테스트")
    class BroadcastSystemNotificationTest {

        @Test
        @DisplayName("전체 유저에게 시스템 알림 브로드캐스트")
        void t1() {
            // given
            String expectedDestination = "/topic/notifications/system";
            NotificationWebSocketDto systemDto = NotificationWebSocketDto.from(
                    2L,
                    "시스템 알림",
                    "시스템 공지",
                    NotificationType.SYSTEM,
                    "/system",
                    LocalDateTime.now()
            );

            // when
            webSocketService.broadcastSystemNotification(systemDto);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq(expectedDestination),
                    eq(systemDto)
            );
        }

        @Test
        @DisplayName("브로드캐스트 실패 시 예외 로깅하고 정상 종료")
        void t2() {
            // given
            willThrow(new RuntimeException("브로드캐스트 실패"))
                    .given(messagingTemplate)
                    .convertAndSend(anyString(), any(Object.class));

            // when & then
            webSocketService.broadcastSystemNotification(notificationDto);

            verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("스터디룸 알림 전송 테스트")
    class SendNotificationToRoomTest {

        @Test
        @DisplayName("스터디룸 멤버들에게 알림 전송")
        void t1() {
            // given
            Long roomId = 100L;
            String expectedDestination = "/topic/room/100/notifications";
            NotificationWebSocketDto roomDto = NotificationWebSocketDto.from(
                    3L,
                    "스터디룸 알림",
                    "새 공지사항",
                    NotificationType.ROOM,
                    "/room/100",
                    LocalDateTime.now()
            );

            // when
            webSocketService.sendNotificationToRoom(roomId, roomDto);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq(expectedDestination),
                    eq(roomDto)
            );
        }

        @Test
        @DisplayName("전송 실패 시 예외 로깅하고 정상 종료")
        void t2() {
            // given
            Long roomId = 100L;
            willThrow(new RuntimeException("룸 전송 실패"))
                    .given(messagingTemplate)
                    .convertAndSend(anyString(), any(Object.class));

            // when & then
            webSocketService.sendNotificationToRoom(roomId, notificationDto);

            verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("destination 검증 테스트")
    class DestinationTest {

        @Test
        @DisplayName("유저별 알림 경로 정상 생성")
        void t1() {
            // given
            Long userId = 12345L;

            // when
            webSocketService.sendNotificationToUser(userId, notificationDto);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/user/12345/notifications"),
                    any(Object.class)
            );
        }

        @Test
        @DisplayName("스터디룸 알림 경로 정상 생성")
        void t2() {
            // given
            Long roomId = 99L;

            // when
            webSocketService.sendNotificationToRoom(roomId, notificationDto);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/99/notifications"),
                    any(Object.class)
            );
        }

        @Test
        @DisplayName("시스템 알림 경로 정상 생성")
        void t3() {
            // when
            webSocketService.broadcastSystemNotification(notificationDto);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/notifications/system"),
                    any(Object.class)
            );
        }
    }
}