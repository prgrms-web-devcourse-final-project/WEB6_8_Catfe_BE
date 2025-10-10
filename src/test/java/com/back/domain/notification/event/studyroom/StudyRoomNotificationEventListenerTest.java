package com.back.domain.notification.event.studyroom;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyRoomNotificationEventListener 테스트")
class StudyRoomNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @InjectMocks
    private StudyRoomNotificationEventListener listener;

    private User actor;
    private User receiver;
    private Room room;

    @BeforeEach
    void setUp() {
        actor = User.builder()
                .username("actor")
                .email("actor@test.com")
                .build();

        receiver = User.builder()
                .username("receiver")
                .email("receiver@test.com")
                .build();

        room = Room.create(
                "테스트 방",
                "설명",
                false,
                null,
                10,
                actor,
                null,
                true,  // useWebRTC
                null   // thumbnailUrl
        );
    }

    // ====================== 스터디룸 공지사항 이벤트 ======================

    @Test
    @DisplayName("스터디룸 공지사항 생성 이벤트 수신 - 알림 생성 성공")
    void t1() {
        // given
        StudyRoomNoticeCreatedEvent event = new StudyRoomNoticeCreatedEvent(
                1L,  // actorId
                100L, // roomId
                "공지사항 제목입니다",
                "공지사항 내용입니다"
        );

        given(roomRepository.findById(100L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        listener.handleNoticeCreated(event);

        // then
        verify(notificationService).createRoomNotification(
                eq(room),
                eq(actor),
                anyString(), // title
                eq("공지사항 제목입니다"), // content (공지 제목)
                eq("/rooms/100/notices"),
                eq(NotificationSettingType.ROOM_NOTICE)
        );
    }

    @Test
    @DisplayName("스터디룸 공지사항 이벤트 - 방 없음")
    void t2() {
        // given
        StudyRoomNoticeCreatedEvent event = new StudyRoomNoticeCreatedEvent(
                1L,
                999L, // 존재하지 않는 roomId
                "공지사항 제목",
                "공지사항 내용입니다"
        );

        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleNoticeCreated(event);

        // then
        verify(notificationService, never()).createRoomNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    @Test
    @DisplayName("스터디룸 공지사항 이벤트 - 작성자 없음")
    void t3() {
        // given
        StudyRoomNoticeCreatedEvent event = new StudyRoomNoticeCreatedEvent(
                999L, // 존재하지 않는 actorId
                100L,
                "공지사항 제목",
                "공지사항 내용입니다"
        );

        given(roomRepository.findById(100L)).willReturn(Optional.of(room));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleNoticeCreated(event);

        // then
        verify(notificationService, never()).createRoomNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    // ====================== 멤버 권한 변경 이벤트 ======================

    @Test
    @DisplayName("멤버 권한 변경 이벤트 수신 - 알림 생성 성공")
    void t4() {
        // given
        MemberRoleChangedEvent event = new MemberRoleChangedEvent(
                1L,  // actorId (권한 변경한 사람)
                100L, // roomId
                2L,  // targetUserId (권한 변경된 사람)
                "MANAGER" // newRole
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        listener.handleMemberRoleChanged(event);

        // then
        verify(notificationService).createPersonalNotification(
                eq(receiver), // 권한 변경된 사람이 수신자
                eq(actor),    // 권한 변경한 사람이 actor
                anyString(),
                anyString(),
                eq("/rooms/100"),
                eq(NotificationSettingType.ROOM_JOIN)
        );
    }

    @Test
    @DisplayName("멤버 권한 변경 이벤트 - 수신자 없음")
    void t5() {
        // given
        MemberRoleChangedEvent event = new MemberRoleChangedEvent(
                1L,
                100L,
                999L, // 존재하지 않는 targetUserId
                "MANAGER"
        );

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleMemberRoleChanged(event);

        // then
        verify(notificationService, never()).createPersonalNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    // ====================== 멤버 추방 이벤트 ======================

    @Test
    @DisplayName("멤버 추방 이벤트 수신 - 알림 생성 성공")
    void t6() {
        // given
        MemberKickedEvent event = new MemberKickedEvent(
                1L,  // actorId (추방한 사람)
                100L, // roomId
                2L,  // kickedUserId (추방당한 사람)
                "테스트 방"
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        listener.handleMemberKicked(event);

        // then
        verify(notificationService).createPersonalNotification(
                eq(receiver), // 추방당한 사람이 수신자
                eq(actor),    // 추방한 사람이 actor
                anyString(),
                anyString(),
                eq("/rooms"), // 방 목록으로 이동
                eq(NotificationSettingType.ROOM_JOIN)
        );
    }

    @Test
    @DisplayName("멤버 추방 이벤트 - 추방당한 사람 없음")
    void t7() {
        // given
        MemberKickedEvent event = new MemberKickedEvent(
                1L,
                100L,
                999L, // 존재하지 않는 kickedUserId
                "테스트 방"
        );

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleMemberKicked(event);

        // then
        verify(notificationService, never()).createPersonalNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    @Test
    @DisplayName("멤버 추방 이벤트 - 추방한 사람 없음")
    void t8() {
        // given
        MemberKickedEvent event = new MemberKickedEvent(
                999L, // 존재하지 않는 actorId
                100L,
                2L,
                "테스트 방"
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleMemberKicked(event);

        // then
        verify(notificationService, never()).createPersonalNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    // ====================== 방장 위임 이벤트 ======================

    @Test
    @DisplayName("방장 위임 이벤트 수신 - 알림 생성 성공")
    void t9() {
        // given
        OwnerTransferredEvent event = new OwnerTransferredEvent(
                1L,  // actorId (이전 방장)
                100L, // roomId
                2L,  // newOwnerId (새 방장)
                "테스트 방"
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        // when
        listener.handleOwnerTransferred(event);

        // then
        verify(notificationService).createPersonalNotification(
                eq(receiver), // 새 방장이 수신자
                eq(actor),    // 이전 방장이 actor
                anyString(),
                anyString(),
                eq("/rooms/100"),
                eq(NotificationSettingType.ROOM_JOIN)
        );
    }

    @Test
    @DisplayName("방장 위임 이벤트 - 새 방장 없음")
    void t10() {
        // given
        OwnerTransferredEvent event = new OwnerTransferredEvent(
                1L,
                100L,
                999L, // 존재하지 않는 newOwnerId
                "테스트 방"
        );

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleOwnerTransferred(event);

        // then
        verify(notificationService, never()).createPersonalNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    @Test
    @DisplayName("방장 위임 이벤트 - 이전 방장 없음")
    void t11() {
        // given
        OwnerTransferredEvent event = new OwnerTransferredEvent(
                999L, // 존재하지 않는 actorId
                100L,
                2L,
                "테스트 방"
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleOwnerTransferred(event);

        // then
        verify(notificationService, never()).createPersonalNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    // ====================== 예외 처리 테스트 ======================

    @Test
    @DisplayName("알림 생성 중 예외 발생 - 로그만 출력하고 예외 전파 안함")
    void t12() {
        // given
        MemberRoleChangedEvent event = new MemberRoleChangedEvent(
                1L, 100L, 2L, "MANAGER"
        );

        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));

        willThrow(new RuntimeException("알림 생성 실패"))
                .given(notificationService).createPersonalNotification(
                        any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
                );

        // when & then - 예외가 전파되지 않아야 함
        assertThatCode(() -> listener.handleMemberRoleChanged(event))
                .doesNotThrowAnyException();

        verify(notificationService).createPersonalNotification(
                any(), any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }
}