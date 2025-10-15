package com.back.global.websocket.service;

import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.event.SessionDisconnectedEvent;
import com.back.global.websocket.event.UserJoinedEvent;
import com.back.global.websocket.event.UserLeftEvent;
import com.back.global.websocket.store.RedisSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomParticipantService 단위 테스트")
class RoomParticipantServiceTest {

    @Mock
    private RedisSessionStore redisSessionStore;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @Spy
    @InjectMocks
    private RoomParticipantService roomParticipantService;

    private Long userId;
    private Long roomId;
    private String username;
    private String sessionId;
    private WebSocketSessionInfo sessionInfo;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = 1L;
        roomId = 100L;
        username = "testUser";
        sessionId = "test-session-123";
        // [FIX] 변경된 DTO의 정적 팩토리 메서드를 사용하여 객체 생성
        sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);
        testUser = User.builder().id(userId).username(username).build();
    }

    @Test
    @DisplayName("방 입장 - 정상 케이스 (첫 입장), 입장 이벤트 방송")
    void enterRoom_FirstTime_BroadcastsUserJoined() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // when
        roomParticipantService.enterRoom(userId, roomId);

        // then
        ArgumentCaptor<WebSocketSessionInfo> sessionCaptor = ArgumentCaptor.forClass(WebSocketSessionInfo.class);
        verify(redisSessionStore).saveUserSession(eq(userId), sessionCaptor.capture());
        verify(redisSessionStore).addUserToRoom(roomId, userId);

        WebSocketSessionInfo updatedSession = sessionCaptor.getValue();
        assertThat(updatedSession.currentRoomId()).isEqualTo(roomId);
        assertThat(updatedSession.username()).isEqualTo(username);

        // 방송 검증
        ArgumentCaptor<UserJoinedEvent> eventCaptor = ArgumentCaptor.forClass(UserJoinedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/" + roomId + "/events"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("방 입장 - 기존 방에서 자동 퇴장 후 새 방 입장, 퇴장/입장 이벤트 모두 방송")
    void enterRoom_SwitchRoom_BroadcastsBothEvents() {
        // given
        Long oldRoomId = 200L;
        WebSocketSessionInfo sessionWithOldRoom = sessionInfo.withRoomId(oldRoomId);
        given(redisSessionStore.getUserSession(anyLong())).willReturn(sessionWithOldRoom);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

        // when
        roomParticipantService.enterRoom(userId, roomId);

        // then
        // 기존 방 퇴장 확인
        verify(redisSessionStore).removeUserFromRoom(oldRoomId, userId);
        // 새 방 입장 확인
        verify(redisSessionStore).addUserToRoom(roomId, userId);

        // 퇴장 방송 검증
        ArgumentCaptor<UserLeftEvent> leftEventCaptor = ArgumentCaptor.forClass(UserLeftEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/" + oldRoomId + "/events"), leftEventCaptor.capture());
        assertThat(leftEventCaptor.getValue().getUserId()).isEqualTo(userId);

        // 입장 방송 검증
        ArgumentCaptor<UserJoinedEvent> joinedEventCaptor = ArgumentCaptor.forClass(UserJoinedEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/" + roomId + "/events"), joinedEventCaptor.capture());
        assertThat(joinedEventCaptor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("방 퇴장 - 정상 케이스, 퇴장 이벤트 방송")
    void exitRoom_Success_BroadcastsUserLeft() {
        // given
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithRoom);

        // when
        roomParticipantService.exitRoom(userId, roomId);

        // then
        verify(redisSessionStore).removeUserFromRoom(roomId, userId);

        // 방송 검증
        ArgumentCaptor<UserLeftEvent> eventCaptor = ArgumentCaptor.forClass(UserLeftEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/" + roomId + "/events"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("세션 종료 이벤트 수신 - 정상적으로 모든 방에서 퇴장 처리")
    void handleSessionDisconnected_ExitsAllRooms() {
        // given
        SessionDisconnectedEvent event = new SessionDisconnectedEvent(this, userId);
        // exitAllRooms가 호출될 것을 기대하므로 doNothing() 처리 (Spy 객체이므로 실제 메서드 호출 방지)
        doNothing().when(roomParticipantService).exitAllRooms(userId);

        // when
        roomParticipantService.handleSessionDisconnected(event);

        // then
        // exitAllRooms가 정확한 userId로 호출되었는지 검증
        verify(roomParticipantService, times(1)).exitAllRooms(userId);
    }

    @Test
    @DisplayName("방 입장 - 세션 정보 없음 (예외 발생)")
    void enterRoom_NoSession_ThrowsException() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> roomParticipantService.enterRoom(userId, roomId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WS_SESSION_NOT_FOUND);

        verify(redisSessionStore, never()).addUserToRoom(anyLong(), anyLong());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("모든 방에서 퇴장 - 현재 방 있음")
    void exitAllRooms_InRoom() {
        // given
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithRoom);

        // when
        roomParticipantService.exitAllRooms(userId);

        // then
        // exitRoom 메서드가 내부적으로 호출되는지 검증 (Spy 객체 활용)
        verify(roomParticipantService, times(1)).exitRoom(userId, roomId);
    }

    @Test
    @DisplayName("모든 방에서 퇴장 - 현재 방 없음")
    void exitAllRooms_NotInRoom() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);

        // when
        roomParticipantService.exitAllRooms(userId);

        // then
        // exitRoom 메서드가 호출되지 않았는지 검증
        verify(roomParticipantService, never()).exitRoom(anyLong(), anyLong());
    }
}
