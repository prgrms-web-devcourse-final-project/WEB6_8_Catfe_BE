package com.back.domain.notification.service;

import com.back.domain.notification.dto.NotificationWebSocketDto;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationRead;
import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.notification.repository.NotificationReadRepository;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.user.entity.User;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationReadRepository notificationReadRepository;

    @Mock
    private NotificationWebSocketService webSocketService;

    @Mock
    private NotificationSettingService notificationSettingService;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private User actor;
    private Room room;
    private Notification notification;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .username("테스터")
                .password("password123")
                .build();

        actor = User.builder()
                .id(2L)
                .email("actor@test.com")
                .username("발신자")
                .password("password123")
                .build();

        room = Room.builder()
                .id(1L)
                .title("테스트룸")
                .description("설명")
                .createdBy(user)
                .build();

        notification = Notification.createPersonalNotification(
                user, actor, "테스트 알림", "내용", "/target"
        );
        ReflectionTestUtils.setField(notification, "id", 1L);
    }

    @Nested
    @DisplayName("알림 생성 및 전송 테스트")
    class CreateNotificationTest {

        @Test
        @DisplayName("개인 알림을 생성하고 WebSocket으로 전송")
        void t1() {
            // given
            given(notificationRepository.save(any(Notification.class)))
                    .willReturn(notification);

            // when
            Notification result = notificationService.createPersonalNotification(
                    user, actor, "테스트 알림", "내용", "/target",
                    NotificationSettingType.SYSTEM
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(NotificationType.PERSONAL);
            assertThat(result.getReceiver()).isEqualTo(user);
            assertThat(result.getActor()).isEqualTo(actor);

            verify(notificationRepository).save(any(Notification.class));
            verify(webSocketService).sendNotificationToUser(
                    eq(user.getId()),
                    any(NotificationWebSocketDto.class)
            );
        }

        @Test
        @DisplayName("스터디룸 알림 생성 - 룸 멤버들에게 전송")
        void t2() {
            // given
            Notification roomNotification = Notification.createRoomNotification(
                    room, actor, "룸 알림", "내용", "/room"
            );
            given(notificationRepository.save(any(Notification.class)))
                    .willReturn(roomNotification);

            // when
            Notification result = notificationService.createRoomNotification(
                    room, actor, "룸 알림", "내용", "/room",
                    NotificationSettingType.ROOM_NOTICE
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(NotificationType.ROOM);
            assertThat(result.getRoom()).isEqualTo(room);
            assertThat(result.getActor()).isEqualTo(actor);

            verify(notificationRepository).save(any(Notification.class));
            verify(webSocketService).sendNotificationToRoom(
                    eq(room.getId()),
                    any(NotificationWebSocketDto.class)
            );
        }

        @Test
        @DisplayName("시스템 알림 생성 - 전체 브로드캐스트")
        void t3() {
            // given
            Notification systemNotification = Notification.createSystemNotification(
                    "시스템 알림", "내용", "/system"
            );
            given(notificationRepository.save(any(Notification.class)))
                    .willReturn(systemNotification);

            // when
            Notification result = notificationService.createSystemNotification(
                    "시스템 알림", "내용", "/system"
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(NotificationType.SYSTEM);
            assertThat(result.getReceiver()).isNull();
            assertThat(result.getRoom()).isNull();

            verify(notificationRepository).save(any(Notification.class));
            verify(webSocketService).broadcastSystemNotification(
                    any(NotificationWebSocketDto.class)
            );
        }

        @Test
        @DisplayName("커뮤니티 알림 생성 - WebSocket으로 전송")
        void t4() {
            // given
            Notification communityNotification = Notification.createCommunityNotification(
                    user, actor, "커뮤니티 알림", "내용", "/community"
            );
            given(notificationRepository.save(any(Notification.class)))
                    .willReturn(communityNotification);
            given(notificationSettingService.isNotificationEnabled(user.getId(), NotificationSettingType.POST_COMMENT))
                    .willReturn(true);

            // when
            Notification result = notificationService.createCommunityNotification(
                    user, actor, "커뮤니티 알림", "내용", "/community",
                    NotificationSettingType.POST_COMMENT
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(NotificationType.COMMUNITY);
            assertThat(result.getReceiver()).isEqualTo(user);
            assertThat(result.getActor()).isEqualTo(actor);

            verify(notificationRepository).save(any(Notification.class));
            verify(notificationSettingService).isNotificationEnabled(user.getId(), NotificationSettingType.POST_COMMENT);
            verify(webSocketService).sendNotificationToUser(
                    eq(user.getId()),
                    any(NotificationWebSocketDto.class)
            );
        }

        @Test
        @DisplayName("자기 자신에게 개인 알림 전송 시 예외 발생")
        void t5() {
            // given
            User sameUser = user;

            // when & then
            assertThatThrownBy(() ->
                    notificationService.createPersonalNotification(
                            sameUser, sameUser, "title", "content", "/url",
                            NotificationSettingType.SYSTEM
                    )
            ).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_FORBIDDEN);

            verify(notificationRepository, never()).save(any(Notification.class));
        }

        @Test
        @DisplayName("자기 자신에게 커뮤니티 알림 전송 시 예외 발생")
        void t6() {
            // given
            User sameUser = user;

            // when & then
            assertThatThrownBy(() ->
                    notificationService.createCommunityNotification(
                            sameUser, sameUser, "title", "content", "/url",
                            NotificationSettingType.POST_COMMENT
                    )
            ).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_FORBIDDEN);

            verify(notificationRepository, never()).save(any(Notification.class));
        }

        @Test
        @DisplayName("알림 설정이 비활성화된 경우 WebSocket 전송 생략")
        void t7() {
            // given
            given(notificationRepository.save(any(Notification.class)))
                    .willReturn(notification);
            given(notificationSettingService.isNotificationEnabled(user.getId(), NotificationSettingType.POST_COMMENT))
                    .willReturn(false);

            // when
            Notification result = notificationService.createCommunityNotification(
                    user, actor, "커뮤니티 알림", "내용", "/community",
                    NotificationSettingType.POST_COMMENT
            );

            // then
            assertThat(result).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
            verify(notificationSettingService).isNotificationEnabled(user.getId(), NotificationSettingType.POST_COMMENT);
            verify(webSocketService, never()).sendNotificationToUser(anyLong(), any());
        }

        @Test
        @DisplayName("자기 자신 알림(createSelfNotification) 생성 성공")
        void t8() {
            // given
            given(notificationRepository.save(any(Notification.class)))
                    .willReturn(notification);

            // when
            Notification result = notificationService.createSelfNotification(
                    user, "학습 기록", "1시간 공부 완료", "/study",
                    NotificationSettingType.SYSTEM
            );

            // then
            assertThat(result).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
            verify(webSocketService).sendNotificationToUser(
                    eq(user.getId()),
                    any(NotificationWebSocketDto.class)
            );
        }
    }

    @Nested
    @DisplayName("알림 조회 테스트")
    class GetNotificationTest {

        @Test
        @DisplayName("유저의 알림 목록 조회")
        void t1() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> expectedPage = new PageImpl<>(List.of(notification));
            given(notificationRepository.findByUserIdOrSystemType(user.getId(), pageable))
                    .willReturn(expectedPage);

            // when
            Page<Notification> result = notificationService.getUserNotifications(
                    user.getId(), pageable
            );

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(notification);
            verify(notificationRepository).findByUserIdOrSystemType(user.getId(), pageable);
        }

        @Test
        @DisplayName("유저의 읽지 않은 알림 목록 조회")
        void t2() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> expectedPage = new PageImpl<>(List.of(notification));
            given(notificationRepository.findUnreadByUserId(user.getId(), pageable))
                    .willReturn(expectedPage);

            // when
            Page<Notification> result = notificationService.getUnreadNotifications(
                    user.getId(), pageable
            );

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(notificationRepository).findUnreadByUserId(user.getId(), pageable);
        }

        @Test
        @DisplayName("유저의 읽지 않은 알림 개수 조회")
        void t3() {
            // given
            given(notificationRepository.countUnreadByUserId(user.getId()))
                    .willReturn(5L);

            // when
            long count = notificationService.getUnreadCount(user.getId());

            // then
            assertThat(count).isEqualTo(5L);
            verify(notificationRepository).countUnreadByUserId(user.getId());
        }

        @Test
        @DisplayName("알림 단건 조회 - 성공")
        void t4() {
            // given
            given(notificationRepository.findById(1L))
                    .willReturn(Optional.of(notification));

            // when
            Notification result = notificationService.getNotification(1L);

            // then
            assertThat(result).isEqualTo(notification);
            verify(notificationRepository).findById(1L);
        }

        @Test
        @DisplayName("알림 단건 조회 - 존재하지 않는 알림")
        void t5() {
            // given
            given(notificationRepository.findById(999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.getNotification(999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);

            verify(notificationRepository).findById(999L);
        }

        @Test
        @DisplayName("알림 읽음 여부 확인")
        void t6() {
            // given
            given(notificationReadRepository.existsByNotificationIdAndUserId(1L, user.getId()))
                    .willReturn(true);

            // when
            boolean result = notificationService.isNotificationRead(1L, user.getId());

            // then
            assertThat(result).isTrue();
            verify(notificationReadRepository).existsByNotificationIdAndUserId(1L, user.getId());
        }
    }

    @Nested
    @DisplayName("알림 읽음 처리 테스트")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림 읽음 처리 - 처음 읽는 알림")
        void t1() {
            // given
            given(notificationRepository.findById(1L))
                    .willReturn(Optional.of(notification));
            given(notificationReadRepository.existsByNotificationIdAndUserId(1L, user.getId()))
                    .willReturn(false);

            // when
            notificationService.markAsRead(1L, user);

            // then
            verify(notificationRepository).findById(1L);
            verify(notificationReadRepository).existsByNotificationIdAndUserId(1L, user.getId());
            verify(notificationReadRepository).save(any(NotificationRead.class));
        }

        @Test
        @DisplayName("알림 읽음 처리 - 이미 읽은 알림")
        void t2() {
            // given
            given(notificationRepository.findById(1L))
                    .willReturn(Optional.of(notification));
            given(notificationReadRepository.existsByNotificationIdAndUserId(1L, user.getId()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(1L, user))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_ALREADY_READ);

            verify(notificationRepository).findById(1L);
            verify(notificationReadRepository).existsByNotificationIdAndUserId(1L, user.getId());
            verify(notificationReadRepository, never()).save(any(NotificationRead.class));
        }

        @Test
        @DisplayName("존재하지 않는 알림 읽음 처리 시 예외 발생")
        void t3() {
            // given
            given(notificationRepository.findById(999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(999L, user))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);

            verify(notificationRepository).findById(999L);
            verify(notificationReadRepository, never()).save(any(NotificationRead.class));
        }

        @Test
        @DisplayName("여러 알림 일괄 읽음 처리")
        void t4() {
            // given
            Notification notification2 = Notification.createPersonalNotification(
                    user, actor, "알림2", "내용2", "/target2"
            );
            ReflectionTestUtils.setField(notification2, "id", 2L);

            List<Notification> unreadList = List.of(notification, notification2);

            given(notificationRepository.findAllUnreadByUserId(user.getId()))
                    .willReturn(unreadList);
            given(notificationReadRepository.existsByNotificationIdAndUserId(anyLong(), eq(user.getId())))
                    .willReturn(false);

            // when
            notificationService.markMultipleAsRead(user.getId(), user);

            // then
            verify(notificationRepository).findAllUnreadByUserId(user.getId());
            verify(notificationReadRepository).saveAll(argThat(list ->
                    list != null && ((List<?>) list).size() == 2
            ));
        }

        @Test
        @DisplayName("일괄 읽음 처리 시 이미 읽은 알림 제외")
        void t5() {
            // given
            Notification notification2 = Notification.createPersonalNotification(
                    user, actor, "알림2", "내용2", "/target2"
            );
            ReflectionTestUtils.setField(notification2, "id", 2L);

            List<Notification> unreadList = List.of(notification, notification2);

            given(notificationRepository.findAllUnreadByUserId(user.getId()))
                    .willReturn(unreadList);
            given(notificationReadRepository.existsByNotificationIdAndUserId(1L, user.getId()))
                    .willReturn(true);
            given(notificationReadRepository.existsByNotificationIdAndUserId(2L, user.getId()))
                    .willReturn(false);

            // when
            notificationService.markMultipleAsRead(user.getId(), user);

            // then
            verify(notificationRepository).findAllUnreadByUserId(user.getId());
            verify(notificationReadRepository).saveAll(argThat(list ->
                    list != null && ((List<?>) list).size() == 1
            ));
        }
    }
}