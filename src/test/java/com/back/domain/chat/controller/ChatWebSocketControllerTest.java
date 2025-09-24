package com.back.domain.chat.controller;

import com.back.domain.chat.dto.ChatMessageDto;
import com.back.domain.chat.service.ChatService;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.global.security.CustomUserDetails;
import com.back.global.websocket.dto.WebSocketErrorResponse;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketController 테스트")
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

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
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        // 리플렉션으로 userProfiles 필드 설정
        setUserProfiles(mockUser, Arrays.asList(mockUserProfile));

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
    private void setUserProfiles(User user, List<UserProfile> profiles) throws Exception {
        Field userProfilesField = User.class.getDeclaredField("userProfiles");
        userProfilesField.setAccessible(true);
        userProfilesField.set(user, profiles);
    }

    // 리플렉션으로 필드 값 설정하는 헬퍼 메소드
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("정상적인 채팅 메시지 처리")
    void t1() {
        // Given
        Long roomId = 1L;
        ChatMessageDto inputMessage = ChatMessageDto.builder()
                .content("테스트 메시지")
                .messageType("TEXT")
                .build();

        // 실제로 필요한 stubbing만 설정
        given(chatService.saveRoomChatMessage(any(ChatMessageDto.class))).willReturn(mockSavedMessage);

        // When
        chatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, testPrincipal);

        // Then
        verify(chatService).saveRoomChatMessage(argThat(dto ->
                dto.getRoomId().equals(roomId) &&
                        dto.getUserId().equals(1L) &&
                        dto.getContent().equals("테스트 메시지")
        ));

        verify(messagingTemplate).convertAndSend(
                eq("/topic/room/" + roomId),
                argThat((ChatMessageDto responseDto) ->
                        responseDto.getMessageId().equals(100L) &&
                                responseDto.getRoomId().equals(roomId) &&
                                responseDto.getUserId().equals(1L) &&
                                responseDto.getNickname().equals("테스터") &&
                                responseDto.getProfileImageUrl().equals("https://example.com/profile.jpg") &&
                                responseDto.getContent().equals("테스트 메시지") &&
                                responseDto.getMessageType().equals("TEXT")
                )
        );

        // 에러 메시지는 전송되지 않아야 함
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 메시지 처리 - 에러 전송")
    void t2() {
        Long roomId = 1L;
        ChatMessageDto inputMessage = ChatMessageDto.builder()
                .content("테스트 메시지")
                .messageType("TEXT")
                .build();

        Principal invalidPrincipal = null; // 인증 정보 없음

        // 에러 응답을 위해 sessionId가 필요
        given(headerAccessor.getSessionId()).willReturn("test-session-123");

        chatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, invalidPrincipal);

        verify(chatService, never()).saveRoomChatMessage(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        // 에러 메시지가 해당 사용자에게만 전송되는지 확인
        verify(messagingTemplate).convertAndSendToUser(
                eq("test-session-123"),
                eq("/queue/errors"),
                argThat((WebSocketErrorResponse errorResponse) ->
                        errorResponse.getError().getCode().equals("WS_UNAUTHORIZED") &&
                                errorResponse.getError().getMessage().equals("인증이 필요합니다")
                )
        );
    }

    @Test
    @DisplayName("서비스 계층 예외 발생 시 에러 처리")
    void t3() {
        Long roomId = 1L;
        ChatMessageDto inputMessage = ChatMessageDto.builder()
                .content("테스트 메시지")
                .messageType("TEXT")
                .build();

        // 예외 발생 시 sessionId와 서비스 예외 설정
        given(headerAccessor.getSessionId()).willReturn("test-session-123");
        given(chatService.saveRoomChatMessage(any())).willThrow(new RuntimeException("DB 오류"));

        chatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, testPrincipal);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        verify(messagingTemplate).convertAndSendToUser(
                eq("test-session-123"),
                eq("/queue/errors"),
                argThat((WebSocketErrorResponse errorResponse) ->
                        errorResponse.getError().getCode().equals("WS_ROOM_NOT_FOUND") &&
                                errorResponse.getError().getMessage().equals("존재하지 않는 방입니다")
                )
        );
    }

    @Test
    @DisplayName("잘못된 Principal 타입 처리")
    void t4() {
        Long roomId = 1L;
        ChatMessageDto inputMessage = ChatMessageDto.builder()
                .content("테스트 메시지")
                .messageType("TEXT")
                .build();

        // Authentication이 아닌 다른 Principal
        Principal invalidTypePrincipal = () -> "some-principal-name";

        given(headerAccessor.getSessionId()).willReturn("test-session-123");

        chatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, invalidTypePrincipal);

        verify(chatService, never()).saveRoomChatMessage(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        verify(messagingTemplate).convertAndSendToUser(
                eq("test-session-123"),
                eq("/queue/errors"),
                any(WebSocketErrorResponse.class)
        );
    }

    @Test
    @DisplayName("CustomUserDetails가 아닌 Principal 객체 처리")
    void t5() {
        Long roomId = 1L;
        ChatMessageDto inputMessage = ChatMessageDto.builder()
                .content("테스트 메시지")
                .messageType("TEXT")
                .build();

        Authentication authWithWrongPrincipal = new UsernamePasswordAuthenticationToken(
                "string-principal", null, null
        );

        given(headerAccessor.getSessionId()).willReturn("test-session-123");

        chatWebSocketController.handleRoomChat(roomId, inputMessage, headerAccessor, authWithWrongPrincipal);

        verify(chatService, never()).saveRoomChatMessage(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        verify(messagingTemplate).convertAndSendToUser(
                eq("test-session-123"),
                eq("/queue/errors"),
                any(WebSocketErrorResponse.class)
        );
    }
}