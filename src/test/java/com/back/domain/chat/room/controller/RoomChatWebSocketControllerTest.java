package com.back.domain.chat.room.controller;

import com.back.domain.chat.room.dto.RoomChatMessageRequest;
import com.back.domain.chat.room.dto.RoomChatMessageResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import com.back.domain.chat.room.service.RoomChatService;
import com.back.global.websocket.util.WebSocketErrorHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomChatWebSocketController 테스트")
class RoomChatWebSocketControllerTest {

    @Mock
    private RoomChatService roomChatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketErrorHelper errorHelper;

    @InjectMocks
    private RoomChatWebSocketController roomChatWebSocketController;

    private Authentication testAuth;
    private Long testUserId = 1L;
    private String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        CustomUserDetails testUserDetails = CustomUserDetails.builder()
                .userId(testUserId)
                .username(testUsername)
                .build();
        testAuth = new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities());
    }

    @Nested
    @DisplayName("handleRoomChat 메서드")
    class HandleRoomChat {

        @Test
        @DisplayName("   - 채팅 메시지를 정상적으로 처리하고 브로드캐스트한다")
        void t1() {
            // given
            Long roomId = 10L;
            RoomChatMessageRequest request = new RoomChatMessageRequest("안녕하세요");

            // Mock User 생성
            User mockUser = User.builder().id(testUserId).build();
            UserProfile mockProfile = UserProfile.builder().nickname("테스터").profileImageUrl("url").build();
            mockUser.setUserProfile(mockProfile); // UserProfile 설정

            // Mock Room 생성
            Room mockRoom = Room.builder().id(roomId).build();

            // Mock RoomChatMessage 생성
            RoomChatMessage savedMessage = RoomChatMessage.builder()
                    .id(100L)
                    .room(mockRoom) // <-- room 필드 설정 추가!
                    .user(mockUser)
                    .content("안녕하세요")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(roomChatService.saveRoomChatMessage(roomId, testUserId, request)).willReturn(savedMessage);

            // when
            roomChatWebSocketController.handleRoomChat(roomId, request, testAuth);

            // then
            verify(roomChatService).saveRoomChatMessage(roomId, testUserId, request);
            verify(messagingTemplate).convertAndSend(eq("/topic/room/" + roomId), any(RoomChatMessageResponse.class));
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("실패 - 인증 정보가 없을 때 Unauthorized 예외를 던진다")
        void t2() {
            // given
            Long roomId = 10L;
            RoomChatMessageRequest request = new RoomChatMessageRequest("안녕하세요");

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    roomChatWebSocketController.handleRoomChat(roomId, request, null)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            verify(roomChatService, never()).saveRoomChatMessage(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 서비스에서 예외 발생 시 해당 예외를 그대로 던진다")
        void t3() {
            // given
            Long roomId = 10L;
            RoomChatMessageRequest request = new RoomChatMessageRequest("안녕하세요");

            given(roomChatService.saveRoomChatMessage(roomId, testUserId, request))
                    .willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    roomChatWebSocketController.handleRoomChat(roomId, request, testAuth)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROOM_NOT_FOUND);
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(RoomChatMessageResponse.class));
        }
    }
}