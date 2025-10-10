package com.back.domain.chat.room.controller;

import com.back.domain.chat.room.dto.ChatClearRequest;
import com.back.domain.chat.room.dto.ChatClearedNotification;
import com.back.domain.chat.room.dto.RoomChatMessageDto;
import com.back.domain.chat.room.service.RoomChatService;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.util.WebSocketErrorHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RoomChatWebSocketControllerTest {

    @Mock
    private RoomChatService roomChatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @Mock
    private WebSocketErrorHelper errorHelper;

    @InjectMocks
    private RoomChatWebSocketController roomChatWebSocketController;

    private CustomUserDetails testUser;
    private Principal testPrincipal;
    private User mockUser;
    private UserProfile mockUserProfile;
    private RoomChatMessage mockSavedMessage;

    @BeforeEach
    void setUp() throws Exception {
        testUser = CustomUserDetails.builder()
                .userId(1L)
                .username("testuser")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        testPrincipal = authentication;

        // UserProfile 생성
        mockUserProfile = createUserProfile("테스터", "https://example.com/profile.jpg");

        // Mock User 객체 생성
        mockUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // 리플렉션으로 userProfile 필드 설정
        setUserProfile(mockUser, mockUserProfile);

        // Mock RoomChatMessage 생성
        mockSavedMessage = RoomChatMessage.builder()
                .id(100L)
                .user(mockUser)
                .content("테스트 메시지")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // UserProfile 생성 헬퍼 메소드
    private UserProfile createUserProfile(String nickname, String profileImageUrl) throws Exception {
        UserProfile userProfile = new UserProfile();

        // 리플렉션으로 private 필드 설정
        setField(userProfile, "nickname", nickname);
        setField(userProfile, "profileImageUrl", profileImageUrl);
        setField(userProfile, "user", mockUser);

        return userProfile;
    }

    // User의 userProfiles 필드 설정
    private void setUserProfile(User user, UserProfile profile) throws Exception {
        Field userProfilesField = User.class.getDeclaredField("userProfile");
        userProfilesField.setAccessible(true);
        userProfilesField.set(user, profile);
    }

    // 리플렉션으로 필드 값 설정하는 헬퍼 메소드
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 조회 성공")
    void t1() {
        // Given
        Long roomId = 1L;

        RoomChatMessageDto inputMessage = RoomChatMessageDto.createRequest(
                "테스트 메시지",
                "TEXT"
        );

        // 실제로 필요한 stubbing만 설정
        given(roomChatService.saveRoomChatMessage(any(RoomChatMessageDto.class))).willReturn(mockSavedMessage);

        // When
        roomChatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, testPrincipal);

        // Then
        verify(roomChatService).saveRoomChatMessage(argThat(dto ->
                dto.roomId().equals(roomId) &&
                        dto.userId().equals(1L) &&
                        dto.content().equals("테스트 메시지")
        ));

        verify(messagingTemplate).convertAndSend(
                eq("/topic/room/" + roomId),
                argThat((RoomChatMessageDto responseDto) ->
                        responseDto.messageId().equals(100L) &&
                                responseDto.roomId().equals(roomId) &&
                                responseDto.userId().equals(1L) &&
                                responseDto.nickname().equals("테스터") &&
                                responseDto.profileImageUrl().equals("https://example.com/profile.jpg") &&
                                responseDto.content().equals("테스트 메시지") &&
                                responseDto.messageType().equals("TEXT")
                )
        );

        // 에러 메시지는 전송되지 않아야 함
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 조회 실패 - 인증되지 않은 사용자의 메시지 처리")
    void t2() {
        Long roomId = 1L;

        RoomChatMessageDto inputMessage = RoomChatMessageDto.createRequest(
                "테스트 메시지",
                "TEXT"
        );

        Principal invalidPrincipal = null; // 인증 정보 없음

        // 에러 응답을 위해 sessionId가 필요
        given(headerAccessor.getSessionId()).willReturn("test-session-123");

        // When
        roomChatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, invalidPrincipal);

        // Then
        verify(roomChatService, never()).saveRoomChatMessage(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        // 에러 메시지가 해당 사용자에게만 전송되는지 확인
        verify(errorHelper).sendUnauthorizedError("test-session-123");
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 조회 실패 - 서비스 계층 예외 발생 시 에러 처리")
    void t3() {
        Long roomId = 1L;

        RoomChatMessageDto inputMessage = RoomChatMessageDto.createRequest(
                "테스트 메시지",
                "TEXT"
        );

        // 예외 발생 시 sessionId와 서비스 예외 설정
        given(headerAccessor.getSessionId()).willReturn("test-session-123");
        given(roomChatService.saveRoomChatMessage(any())).willThrow(new RuntimeException("DB 오류"));

        // When
        roomChatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, testPrincipal);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        verify(errorHelper).sendGenericErrorToUser(
                eq("test-session-123"),
                any(RuntimeException.class),
                eq("메시지 전송 중 오류가 발생했습니다")
        );
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 조회 실패 - 잘못된 Principal 타입 처리")
    void t4() {
        Long roomId = 1L;

        RoomChatMessageDto inputMessage = RoomChatMessageDto.createRequest(
                "테스트 메시지",
                "TEXT"
        );

        // Authentication이 아닌 다른 Principal
        Principal invalidTypePrincipal = () -> "some-principal-name";

        given(headerAccessor.getSessionId()).willReturn("test-session-123");

        // When
        roomChatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, invalidTypePrincipal);

        // Then
        verify(roomChatService, never()).saveRoomChatMessage(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        // 실제 호출되는 sendUnauthorizedError 검증
        verify(errorHelper).sendUnauthorizedError("test-session-123");
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 조회 실패 - CustomUserDetails가 아닌 Principal 객체 처리")
    void t5() {
        Long roomId = 1L;

        RoomChatMessageDto inputMessage = RoomChatMessageDto.createRequest(
                "테스트 메시지",
                "TEXT"
        );

        Authentication authWithWrongPrincipal = new UsernamePasswordAuthenticationToken(
                "string-principal", null, null
        );

        given(headerAccessor.getSessionId()).willReturn("test-session-123");

        // When
        roomChatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, authWithWrongPrincipal);

        // Then
        verify(roomChatService, never()).saveRoomChatMessage(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        verify(errorHelper).sendUnauthorizedError("test-session-123");
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 삭제 성공 - 방장 권한")
    void t6() {
        Long roomId = 1L;
        int messageCount = 25;
        ChatClearRequest request = new ChatClearRequest("모든 채팅을 삭제하겠습니다");

        ChatClearedNotification.ClearedByDto clearedByInfo =
                new ChatClearedNotification.ClearedByDto(1L, "방장", "https://example.com/host.jpg", "HOST");

        given(roomChatService.getRoomChatCount(roomId)).willReturn(messageCount);
        given(roomChatService.clearRoomChat(roomId, 1L)).willReturn(clearedByInfo);

        roomChatWebSocketController.clearRoomChat(roomId, request, headerAccessor, testPrincipal);

        verify(messagingTemplate).convertAndSend(eq("/topic/room/" + roomId + "/chat-cleared"),
                argThat((ChatClearedNotification notification) ->
                        notification.roomId().equals(roomId) &&
                                notification.deletedCount().equals(messageCount) &&
                                notification.clearedBy().userId().equals(1L) &&
                                notification.clearedBy().nickname().equals("방장") &&
                                notification.clearedBy().role().equals("HOST")
                ));
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 삭제 성공 - 부방장 권한")
    void t7() {
        Long roomId = 2L;
        int messageCount = 10;

        ChatClearRequest request = new ChatClearRequest("모든 채팅을 삭제하겠습니다");

        ChatClearedNotification.ClearedByDto clearedByInfo = new ChatClearedNotification.ClearedByDto(
                1L, "부방장", "https://example.com/subhost.jpg", "SUB_HOST"
        );

        // Mock 설정
        given(roomChatService.getRoomChatCount(roomId)).willReturn(messageCount);
        given(roomChatService.clearRoomChat(roomId, 1L)).willReturn(clearedByInfo);

        // When
        roomChatWebSocketController.clearRoomChat(roomId, request, headerAccessor, testPrincipal);

        // Then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/room/" + roomId + "/chat-cleared"),
                argThat((ChatClearedNotification notification) ->
                        notification.clearedBy().role().equals("SUB_HOST") &&
                                notification.clearedBy().nickname().equals("부방장")
                )
        );
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 삭제 실패 - 인증되지 않은 사용자")
    void t8() {
        Long roomId = 1L;
        ChatClearRequest request = new ChatClearRequest("모든 채팅을 삭제하겠습니다");
        Principal invalidPrincipal = null;

        given(headerAccessor.getSessionId()).willReturn("test-session-123");

        // When
        roomChatWebSocketController.clearRoomChat(roomId, request, headerAccessor, invalidPrincipal);

        // Then
        verify(roomChatService, never()).getRoomChatCount(any());
        verify(roomChatService, never()).clearRoomChat(any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        verify(errorHelper).sendUnauthorizedError("test-session-123");
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 삭제 실패 - 잘못된 확인 메시지")
    void t9() {
        Long roomId = 1L;
        ChatClearRequest invalidRequest = new ChatClearRequest("잘못된 확인 메시지");

        given(headerAccessor.getSessionId()).willReturn("test-session-123");

        // When
        roomChatWebSocketController.clearRoomChat(roomId, invalidRequest, headerAccessor, testPrincipal);

        // Then
        verify(roomChatService, never()).getRoomChatCount(any());
        verify(roomChatService, never()).clearRoomChat(any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        verify(errorHelper).sendErrorToUser(
                eq("test-session-123"),
                eq("WS_011"),
                eq("삭제 확인 메시지가 일치하지 않습니다")
        );
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 삭제 실패 - 권한 없음 (일반 멤버)")
    void t10() {
        Long roomId = 1L;
        ChatClearRequest request = new ChatClearRequest("모든 채팅을 삭제하겠습니다");

        given(headerAccessor.getSessionId()).willReturn("test-session-123");
        given(roomChatService.getRoomChatCount(roomId)).willReturn(5);
        given(roomChatService.clearRoomChat(roomId, 1L))
                .willThrow(new SecurityException("채팅 삭제 권한이 없습니다"));

        // When
        roomChatWebSocketController.clearRoomChat(roomId, request, headerAccessor, testPrincipal);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        verify(errorHelper).sendGenericErrorToUser(
                eq("test-session-123"),
                any(SecurityException.class),
                eq("채팅 삭제 중 오류가 발생했습니다")
        );
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 삭제 실패 - 방 멤버가 아님")
    void t11() {
        Long roomId = 1L;
        ChatClearRequest request = new ChatClearRequest("모든 채팅을 삭제하겠습니다");

        given(headerAccessor.getSessionId()).willReturn("test-session-123");
        given(roomChatService.getRoomChatCount(roomId)).willReturn(5);
        given(roomChatService.clearRoomChat(roomId, 1L))
                .willThrow(new CustomException(ErrorCode.NOT_ROOM_MEMBER));

        // When
        roomChatWebSocketController.clearRoomChat(roomId, request, headerAccessor, testPrincipal);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        verify(errorHelper).sendCustomExceptionToUser(
                eq("test-session-123"),
                any(CustomException.class)
        );
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 삭제 실패 - 존재하지 않는 방")
    void t12() {
        Long roomId = 999L;
        ChatClearRequest request = new ChatClearRequest("모든 채팅을 삭제하겠습니다");

        given(headerAccessor.getSessionId()).willReturn("test-session-123");
        given(roomChatService.getRoomChatCount(roomId)).willReturn(0);
        given(roomChatService.clearRoomChat(roomId, 1L))
                .willThrow(new IllegalArgumentException("존재하지 않는 방입니다"));

        // When
        roomChatWebSocketController.clearRoomChat(roomId, request, headerAccessor, testPrincipal);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        verify(errorHelper).sendGenericErrorToUser(
                eq("test-session-123"),
                any(IllegalArgumentException.class),
                eq("채팅 삭제 중 오류가 발생했습니다")
        );
    }

    @Test
    @DisplayName("WebSocket 채팅 전체 삭제 - 메시지 수가 0인 경우")
    void t13() {
        Long roomId = 3L;
        int messageCount = 0; // 메시지가 없는 경우

        ChatClearRequest request = new ChatClearRequest("모든 채팅을 삭제하겠습니다");

        ChatClearedNotification.ClearedByDto clearedByInfo = new ChatClearedNotification.ClearedByDto(
                1L, "방장", "https://example.com/host.jpg", "HOST"
        );

        // Mock 설정
        given(roomChatService.getRoomChatCount(roomId)).willReturn(messageCount);
        given(roomChatService.clearRoomChat(roomId, 1L)).willReturn(clearedByInfo);

        // When
        roomChatWebSocketController.clearRoomChat(roomId, request, headerAccessor, testPrincipal);

        // Then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/room/" + roomId + "/chat-cleared"),
                argThat((ChatClearedNotification notification) ->
                        notification.deletedCount().equals(0) &&
                                notification.message().contains("방장님이 모든 채팅을 삭제했습니다.")
                )
        );
    }
}