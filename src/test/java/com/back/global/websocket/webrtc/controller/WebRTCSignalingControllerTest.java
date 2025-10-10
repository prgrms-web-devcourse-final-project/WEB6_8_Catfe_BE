package com.back.global.websocket.webrtc.controller;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaStateResponse;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaToggleRequest;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaType;
import com.back.global.websocket.webrtc.dto.signal.*;
import com.back.global.websocket.webrtc.service.WebRTCSignalValidator;
import com.back.global.websocket.util.WebSocketErrorHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebRTC 시그널링 컨트롤러")
class WebRTCSignalingControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketErrorHelper errorHelper;

    @Mock
    private WebRTCSignalValidator validator;

    @Mock
    private WebSocketSessionManager sessionManager;

    @InjectMocks
    private WebRTCSignalingController controller;

    private SimpMessageHeaderAccessor headerAccessor;
    private Authentication authentication;
    private Long roomId;
    private Long fromUserId;
    private Long targetUserId;
    private String fromSessionId;
    private String targetSessionId;

    @BeforeEach
    void setUp() {
        roomId = 1L;
        fromUserId = 10L;
        targetUserId = 20L;
        fromSessionId = "from-session-id";
        targetSessionId = "target-session-id";

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(fromUserId);
        lenient().when(userDetails.getUsername()).thenReturn("testUser");
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, null);

        headerAccessor = mock(SimpMessageHeaderAccessor.class);
        lenient().when(headerAccessor.getSessionId()).thenReturn(fromSessionId);
    }

    @Nested
    @DisplayName("Offer 처리")
    class HandleOfferTest {

        @Test
        @DisplayName("성공 - Offer 메시지를 특정 사용자에게 전송")
        void t1() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(roomId, targetUserId, "sdp", WebRTCMediaType.AUDIO);
            WebSocketSessionInfo targetSession = new WebSocketSessionInfo(targetUserId, targetSessionId, LocalDateTime.now(), LocalDateTime.now(), null);
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(targetSession);

            // when
            controller.handleOffer(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verify(messagingTemplate).convertAndSendToUser(eq(targetSessionId), eq("/queue/webrtc"), any(WebRTCSignalResponse.class));
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("실패 - 대상 사용자가 오프라인일 때 예외를 던짐")
        void t2() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(roomId, targetUserId, "sdp", WebRTCMediaType.AUDIO);
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(null);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    controller.handleOffer(request, headerAccessor, authentication)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WS_TARGET_OFFLINE);

            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verifyNoInteractions(errorHelper);
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("실패 - 인증 정보 없음")
        void t3() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(roomId, targetUserId, "sdp", WebRTCMediaType.AUDIO);

            // when
            controller.handleOffer(request, headerAccessor, null);

            // then
            verify(errorHelper).sendUnauthorizedError(fromSessionId);
            verify(validator, never()).validateSignal(any(), any(), any());
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("실패 - 검증 오류 시 예외를 던짐")
        void t4() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(roomId, targetUserId, "sdp", WebRTCMediaType.VIDEO);
            doThrow(new CustomException(ErrorCode.BAD_REQUEST)).when(validator).validateSignal(roomId, fromUserId, targetUserId);

            // when & then
            assertThrows(CustomException.class, () -> controller.handleOffer(request, headerAccessor, authentication));
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verifyNoInteractions(errorHelper);
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Answer 처리")
    class HandleAnswerTest {

        @Test
        @DisplayName("성공 - Answer 메시지를 특정 사용자에게 전송")
        void t1() {
            // given
            WebRTCAnswerRequest request = new WebRTCAnswerRequest(roomId, targetUserId, "sdp", WebRTCMediaType.AUDIO);
            WebSocketSessionInfo targetSession = new WebSocketSessionInfo(targetUserId, targetSessionId, LocalDateTime.now(), LocalDateTime.now(), null);
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(targetSession);

            // when
            controller.handleAnswer(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verify(messagingTemplate).convertAndSendToUser(eq(targetSessionId), eq("/queue/webrtc"), any(WebRTCSignalResponse.class));
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("실패 - 대상 사용자가 오프라인일 때 예외를 던짐")
        void t2() {
            // given
            WebRTCAnswerRequest request = new WebRTCAnswerRequest(roomId, targetUserId, "sdp", WebRTCMediaType.AUDIO);
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(null);

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    controller.handleAnswer(request, headerAccessor, authentication)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WS_TARGET_OFFLINE);

            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verifyNoInteractions(errorHelper);
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("ICE Candidate 처리")
    class HandleIceCandidateTest {

        @Test
        @DisplayName("성공 - ICE Candidate를 특정 사용자에게 전송")
        void t1() {
            // given
            WebRTCIceCandidateRequest request = new WebRTCIceCandidateRequest(roomId, targetUserId, "candidate", "audio", 0);
            WebSocketSessionInfo targetSession = new WebSocketSessionInfo(targetUserId, targetSessionId, LocalDateTime.now(), LocalDateTime.now(), null);
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(targetSession);

            // when
            controller.handleIceCandidate(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verify(messagingTemplate).convertAndSendToUser(eq(targetSessionId), eq("/queue/webrtc"), any(WebRTCSignalResponse.class));
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("성공 - 대상 사용자가 오프라인이어도 조용히 무시")
        void t2() {
            // given
            WebRTCIceCandidateRequest request = new WebRTCIceCandidateRequest(roomId, targetUserId, "candidate", "audio", 0);
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(null);

            // when
            controller.handleIceCandidate(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verifyNoInteractions(errorHelper);
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("미디어 상태 토글")
    class HandleMediaToggleTest {

        @Test
        @DisplayName("성공 - 미디어 상태를 방 전체에 브로드캐스트")
        void t1() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(roomId, WebRTCMediaType.AUDIO, true);
            doNothing().when(validator).validateMediaStateChange(roomId, fromUserId);

            // when
            controller.handleMediaToggle(request, headerAccessor, authentication);

            // then
            verify(validator).validateMediaStateChange(roomId, fromUserId);
            verify(messagingTemplate).convertAndSend(eq("/topic/room/" + roomId + "/media-status"), any(WebRTCMediaStateResponse.class));
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("실패 - 검증 오류 시 예외를 던짐")
        void t2() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(roomId, WebRTCMediaType.SCREEN, true);
            doThrow(new RuntimeException("검증 실패")).when(validator).validateMediaStateChange(roomId, fromUserId);

            // when & then
            assertThrows(RuntimeException.class, () -> controller.handleMediaToggle(request, headerAccessor, authentication));
            verify(validator).validateMediaStateChange(roomId, fromUserId);
            verifyNoInteractions(errorHelper);
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(WebRTCMediaStateResponse.class));
        }
    }
}