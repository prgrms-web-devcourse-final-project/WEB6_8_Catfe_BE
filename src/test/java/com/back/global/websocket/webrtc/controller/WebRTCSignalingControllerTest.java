package com.back.global.websocket.webrtc.controller;

import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.service.WebSocketSessionManager;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
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
    private CustomUserDetails userDetails;
    private Authentication authentication;
    private Long roomId;
    private Long fromUserId;
    private Long targetUserId;
    private String targetSessionId;

    @BeforeEach
    void setUp() {
        roomId = 1L;
        fromUserId = 10L;
        targetUserId = 20L;
        targetSessionId = "target-session-id";

        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(fromUserId);
        lenient().when(userDetails.getUsername()).thenReturn("testUser");

        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, null);

        headerAccessor = mock(SimpMessageHeaderAccessor.class);
        lenient().when(headerAccessor.getSessionId()).thenReturn("test-session-id");
    }

    @Nested
    @DisplayName("Offer 처리")
    class HandleOfferTest {

        @Test
        @DisplayName("성공 - Offer 메시지를 특정 사용자에게 전송")
        void t1() {
            // given
            String sdp = "test-sdp-offer";
            WebRTCOfferRequest request = new WebRTCOfferRequest(roomId, targetUserId, sdp, WebRTCMediaType.AUDIO);
            WebSocketSessionInfo targetSession = new WebSocketSessionInfo(targetUserId, targetSessionId, LocalDateTime.now(), LocalDateTime.now(), null);

            // sessionManager가 targetSession을 반환하도록 설정
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(targetSession);

            // when
            controller.handleOffer(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);

            // convertAndSendToUser를 검증하도록 변경
            ArgumentCaptor<String> sessionIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<WebRTCSignalResponse> responseCaptor = ArgumentCaptor.forClass(WebRTCSignalResponse.class);
            verify(messagingTemplate).convertAndSendToUser(
                    sessionIdCaptor.capture(),
                    destinationCaptor.capture(),
                    responseCaptor.capture()
            );

            assertThat(sessionIdCaptor.getValue()).isEqualTo(targetSessionId);
            assertThat(destinationCaptor.getValue()).isEqualTo("/queue/webrtc");

            WebRTCSignalResponse response = responseCaptor.getValue();
            assertThat(response.type()).isEqualTo(WebRTCSignalType.OFFER);
            assertThat(response.fromUserId()).isEqualTo(fromUserId);
            assertThat(response.targetUserId()).isEqualTo(targetUserId);
        }

        @Test
        @DisplayName("실패 - 대상 사용자가 오프라인")
        void t2() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(roomId, targetUserId, "sdp", WebRTCMediaType.AUDIO);

            // sessionManager가 null을 반환하도록 설정
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(null);

            // when
            controller.handleOffer(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verify(errorHelper).sendErrorToUser(eq("test-session-id"), eq("WEBRTC_TARGET_OFFLINE"), anyString());
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }


        @Test
        @DisplayName("실패 - 인증 정보 없음")
        void t3() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(
                    roomId,
                    targetUserId,
                    "test-sdp",
                    WebRTCMediaType.AUDIO
            );
            Principal invalidPrincipal = mock(Principal.class);

            // when
            controller.handleOffer(request, headerAccessor, invalidPrincipal);

            // then
            verify(errorHelper).sendUnauthorizedError("test-session-id");
            verify(validator, never()).validateSignal(any(), any(), any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("실패 - 검증 오류")
        void t4() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(
                    roomId,
                    targetUserId,
                    "test-sdp",
                    WebRTCMediaType.VIDEO
            );

            doThrow(new RuntimeException("검증 실패"))
                    .when(validator).validateSignal(roomId, fromUserId, targetUserId);

            // when
            controller.handleOffer(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verify(errorHelper).sendGenericErrorToUser(
                    eq("test-session-id"),
                    any(Exception.class),
                    eq("Offer 전송 중 오류가 발생했습니다")
            );
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("Answer 처리")
    class HandleAnswerTest {

        @Test
        @DisplayName("성공 - Answer 메시지를 특정 사용자에게 전송")
        void t5() {
            // given
            String sdp = "test-sdp-answer";
            WebRTCAnswerRequest request = new WebRTCAnswerRequest(roomId, targetUserId, sdp, WebRTCMediaType.SCREEN);
            WebSocketSessionInfo targetSession = new WebSocketSessionInfo(targetUserId, targetSessionId, LocalDateTime.now(), LocalDateTime.now(), null);
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(targetSession);

            // when
            controller.handleAnswer(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);

            ArgumentCaptor<String> sessionIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<WebRTCSignalResponse> responseCaptor = ArgumentCaptor.forClass(WebRTCSignalResponse.class);
            verify(messagingTemplate).convertAndSendToUser(
                    sessionIdCaptor.capture(),
                    destinationCaptor.capture(),
                    responseCaptor.capture()
            );

            assertThat(sessionIdCaptor.getValue()).isEqualTo(targetSessionId);
            assertThat(destinationCaptor.getValue()).isEqualTo("/queue/webrtc");

            WebRTCSignalResponse response = responseCaptor.getValue();
            assertThat(response.type()).isEqualTo(WebRTCSignalType.ANSWER);
        }

        @Test
        @DisplayName("실패 - 인증 정보 없음")
        void t6() {
            // given
            WebRTCAnswerRequest request = new WebRTCAnswerRequest(
                    roomId,
                    targetUserId,
                    "test-sdp",
                    WebRTCMediaType.AUDIO
            );
            Principal invalidPrincipal = mock(Principal.class);

            // when
            controller.handleAnswer(request, headerAccessor, invalidPrincipal);

            // then
            verify(errorHelper).sendUnauthorizedError("test-session-id");
            verify(validator, never()).validateSignal(any(), any(), any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("ICE Candidate 처리")
    class HandleIceCandidateTest {

        @Test
        @DisplayName("성공 - ICE Candidate를 특정 사용자에게 전송")
        void t7() {
            // given
            WebRTCIceCandidateRequest request = new WebRTCIceCandidateRequest(roomId, targetUserId, "candidate:123", "audio", 0);
            WebSocketSessionInfo targetSession = new WebSocketSessionInfo(targetUserId, targetSessionId, LocalDateTime.now(), LocalDateTime.now(), null);
            when(sessionManager.getSessionInfo(targetUserId)).thenReturn(targetSession);

            // when
            controller.handleIceCandidate(request, headerAccessor, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);

            ArgumentCaptor<String> sessionIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<WebRTCSignalResponse> responseCaptor = ArgumentCaptor.forClass(WebRTCSignalResponse.class);
            verify(messagingTemplate).convertAndSendToUser(
                    sessionIdCaptor.capture(),
                    destinationCaptor.capture(),
                    responseCaptor.capture()
            );

            assertThat(sessionIdCaptor.getValue()).isEqualTo(targetSessionId);
            assertThat(destinationCaptor.getValue()).isEqualTo("/queue/webrtc");

            WebRTCSignalResponse response = responseCaptor.getValue();
            assertThat(response.type()).isEqualTo(WebRTCSignalType.ICE_CANDIDATE);
            assertThat(response.candidate()).isEqualTo("candidate:123");
        }

        @Test
        @DisplayName("실패 - 검증 오류")
        void t8() {
            // given
            WebRTCIceCandidateRequest request = new WebRTCIceCandidateRequest(
                    roomId,
                    targetUserId,
                    "candidate",
                    "audio",
                    0
            );

            doThrow(new RuntimeException("검증 실패"))
                    .when(validator).validateSignal(roomId, fromUserId, targetUserId);

            // when
            controller.handleIceCandidate(request, headerAccessor, authentication);

            // then
            verify(errorHelper).sendGenericErrorToUser(
                    eq("test-session-id"),
                    any(Exception.class),
                    eq("ICE Candidate 전송 중 오류가 발생했습니다")
            );
        }
    }

    @Nested
    @DisplayName("미디어 상태 토글")
    class HandleMediaToggleTest {

        @Test
        @DisplayName("정상 - 미디어 상태를 방 전체에 브로드캐스트")
        void t9() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(roomId, WebRTCMediaType.AUDIO, true);
            doNothing().when(validator).validateMediaStateChange(roomId, fromUserId);

            // when
            controller.handleMediaToggle(request, headerAccessor, authentication);

            // then
            ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(
                    destinationCaptor.capture(),
                    payloadCaptor.capture()
            );

            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
            assertThat(destinationCaptor.getValue()).isEqualTo("/topic/room/" + roomId + "/media-status");
        }

        @Test
        @DisplayName("정상 - 오디오 활성화")
        void t10() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(
                    roomId,
                    WebRTCMediaType.AUDIO,
                    true
            );

            doNothing().when(validator).validateMediaStateChange(roomId, fromUserId);

            // when
            controller.handleMediaToggle(request, headerAccessor, authentication);

            // then
            verify(validator).validateMediaStateChange(roomId, fromUserId);

            ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(
                    destinationCaptor.capture(),
                    payloadCaptor.capture()
            );

            assertThat(destinationCaptor.getValue()).isEqualTo("/topic/room/" + roomId + "/media-status");
        }

        @Test
        @DisplayName("정상 - 비디오 비활성화")
        void t11() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(
                    roomId,
                    WebRTCMediaType.VIDEO,
                    false
            );

            doNothing().when(validator).validateMediaStateChange(roomId, fromUserId);

            // when
            controller.handleMediaToggle(request, headerAccessor, authentication);

            // then
            verify(validator).validateMediaStateChange(roomId, fromUserId);

            ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(
                    destinationCaptor.capture(),
                    payloadCaptor.capture()
            );

            assertThat(destinationCaptor.getValue()).isEqualTo("/topic/room/" + roomId + "/media-status");
        }

        @Test
        @DisplayName("실패 - 인증 정보 없음")
        void t12() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(
                    roomId,
                    WebRTCMediaType.AUDIO,
                    true
            );
            Principal invalidPrincipal = mock(Principal.class);

            // when
            controller.handleMediaToggle(request, headerAccessor, invalidPrincipal);

            // then
            verify(errorHelper).sendUnauthorizedError("test-session-id");
            verify(validator, never()).validateMediaStateChange(any(), any());
        }

        @Test
        @DisplayName("실패 - 검증 오류")
        void t13() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(
                    roomId,
                    WebRTCMediaType.SCREEN,
                    true
            );

            doThrow(new RuntimeException("검증 실패"))
                    .when(validator).validateMediaStateChange(roomId, fromUserId);

            // when
            controller.handleMediaToggle(request, headerAccessor, authentication);

            // then
            verify(errorHelper).sendGenericErrorToUser(
                    eq("test-session-id"),
                    any(Exception.class),
                    eq("미디어 상태 변경 중 오류가 발생했습니다")
            );
        }
    }
}