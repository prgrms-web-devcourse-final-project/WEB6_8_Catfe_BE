package com.back.global.websocket.webrtc.controller;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.webrtc.dto.WebRTCErrorResponse;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaStateResponse;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaToggleRequest;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaType;
import com.back.global.websocket.webrtc.dto.signal.*;
import com.back.global.websocket.webrtc.service.WebRTCSignalValidator;
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
    private WebRTCSignalValidator validator;

    @InjectMocks
    private WebRTCSignalingController controller;

    private Authentication authentication;
    private Long roomId;
    private Long fromUserId;
    private String fromUsername;
    private Long targetUserId;
    private String targetUsername;
    private String sdpContent;

    @BeforeEach
    void setUp() {
        roomId = 1L;
        fromUserId = 10L;
        fromUsername = "userA";
        targetUserId = 20L;
        targetUsername = "userB";
        sdpContent = "v=0\r\no=- 123456789 2 IN IP4 127.0.0.1\r\n...";

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(fromUserId);
        lenient().when(userDetails.getUsername()).thenReturn(fromUsername);
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, null);
    }

    @Nested
    @DisplayName("Offer 처리")
    class HandleOfferTest {

        @Test
        @DisplayName("성공 - Offer 메시지를 방 전체에 브로드캐스트")
        void t1() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(
                    roomId,
                    targetUserId,
                    sdpContent,
                    WebRTCMediaType.AUDIO
            );

            // when
            controller.handleOffer(request, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/webrtc"),
                    (WebRTCSignalResponse) argThat((WebRTCSignalResponse response) ->
                            response.type() == WebRTCSignalType.OFFER &&
                                    response.fromUserId().equals(fromUserId) &&
                                    response.targetUserId().equals(targetUserId) &&
                                    response.roomId().equals(roomId) &&
                                    response.sdp().equals(sdpContent) &&
                                    response.mediaType() == WebRTCMediaType.AUDIO
                    )
            );
        }

        @Test
        @DisplayName("실패 - 인증 정보가 없을 때 Unauthorized 예외를 던짐")
        void t2() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(
                    roomId,
                    targetUserId,
                    sdpContent,
                    WebRTCMediaType.AUDIO
            );

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    controller.handleOffer(request, null)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            verify(validator, never()).validateSignal(any(), any(), any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("실패 - Validator에서 예외 발생 시 메시지를 전송하지 않음")
        void t3() {
            // given
            WebRTCOfferRequest request = new WebRTCOfferRequest(
                    roomId,
                    targetUserId,
                    sdpContent,
                    WebRTCMediaType.AUDIO
            );
            doThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND))
                    .when(validator).validateSignal(roomId, fromUserId, targetUserId);

            // when & then
            assertThrows(CustomException.class, () ->
                    controller.handleOffer(request, authentication)
            );
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("Answer 처리")
    class HandleAnswerTest {

        @Test
        @DisplayName("성공 - Answer 메시지를 방 전체에 브로드캐스트")
        void t1() {
            // given
            WebRTCAnswerRequest request = new WebRTCAnswerRequest(
                    roomId,
                    targetUserId,
                    sdpContent,
                    WebRTCMediaType.AUDIO
            );

            // when
            controller.handleAnswer(request, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/webrtc"),
                    (WebRTCSignalResponse) argThat((WebRTCSignalResponse response) ->
                            response.type() == WebRTCSignalType.ANSWER &&
                                    response.fromUserId().equals(fromUserId) &&
                                    response.targetUserId().equals(targetUserId) &&
                                    response.roomId().equals(roomId) &&
                                    response.sdp().equals(sdpContent) &&
                                    response.mediaType() == WebRTCMediaType.AUDIO
                    )
            );
        }

        @Test
        @DisplayName("실패 - 인증 정보가 없을 때 Unauthorized 예외를 던짐")
        void t2() {
            // given
            WebRTCAnswerRequest request = new WebRTCAnswerRequest(
                    roomId,
                    targetUserId,
                    sdpContent,
                    WebRTCMediaType.AUDIO
            );

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    controller.handleAnswer(request, null)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            verify(validator, never()).validateSignal(any(), any(), any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("ICE Candidate 처리")
    class HandleIceCandidateTest {

        @Test
        @DisplayName("성공 - ICE Candidate를 방 전체에 브로드캐스트")
        void t1() {
            // given
            String candidateValue = "candidate:1 1 UDP 2130706431 192.168.1.1 54321 typ host";
            WebRTCIceCandidateRequest request = new WebRTCIceCandidateRequest(
                    roomId,
                    targetUserId,
                    candidateValue,
                    "audio",
                    0
            );

            // when
            controller.handleIceCandidate(request, authentication);

            // then
            verify(validator).validateSignal(roomId, fromUserId, targetUserId);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/webrtc"),
                    (WebRTCSignalResponse) argThat((WebRTCSignalResponse response) ->
                            response.type() == WebRTCSignalType.ICE_CANDIDATE &&
                                    response.fromUserId().equals(fromUserId) &&
                                    response.targetUserId().equals(targetUserId) &&
                                    response.roomId().equals(roomId) &&
                                    response.candidate().equals(candidateValue) &&
                                    response.sdpMid().equals("audio") &&
                                    response.sdpMLineIndex() == 0
                    )
            );
        }

        @Test
        @DisplayName("실패 - 인증 정보가 없을 때 Unauthorized 예외를 던짐")
        void t2() {
            // given
            WebRTCIceCandidateRequest request = new WebRTCIceCandidateRequest(
                    roomId,
                    targetUserId,
                    "candidate",
                    "audio",
                    0
            );

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    controller.handleIceCandidate(request, null)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            verify(validator, never()).validateSignal(any(), any(), any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("미디어 상태 토글")
    class HandleMediaToggleTest {

        @Test
        @DisplayName("성공 - 미디어 상태를 방 전체에 브로드캐스트")
        void t1() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(
                    roomId,
                    WebRTCMediaType.AUDIO,
                    true
            );

            // when
            controller.handleMediaToggle(request, authentication);

            // then
            verify(validator).validateMediaStateChange(roomId, fromUserId);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/media-status"),
                    (WebRTCMediaStateResponse) argThat((WebRTCMediaStateResponse response) ->
                            response.userId().equals(fromUserId) &&
                                    response.nickname().equals(fromUsername) &&
                                    response.mediaType() == WebRTCMediaType.AUDIO &&
                                    response.enabled()
                    )
            );
        }

        @Test
        @DisplayName("실패 - 인증 정보가 없을 때 Unauthorized 예외를 던짐")
        void t2() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(
                    roomId,
                    WebRTCMediaType.AUDIO,
                    true
            );

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    controller.handleMediaToggle(request, null)
            );
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
            verify(validator, never()).validateMediaStateChange(any(), any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("실패 - Validator에서 예외 발생 시 메시지를 전송하지 않음")
        void t3() {
            // given
            WebRTCMediaToggleRequest request = new WebRTCMediaToggleRequest(
                    roomId,
                    WebRTCMediaType.AUDIO,
                    true
            );
            doThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND))
                    .when(validator).validateMediaStateChange(roomId, fromUserId);

            // when & then
            assertThrows(CustomException.class, () ->
                    controller.handleMediaToggle(request, authentication)
            );
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandlerTest {

        @Test
        @DisplayName("CustomException 처리 - 사용자 큐로 에러 전송")
        void t1() {
            // given
            CustomException exception = new CustomException(ErrorCode.ROOM_NOT_FOUND);

            // when
            controller.handleCustomException(exception, authentication);

            // then
            verify(messagingTemplate).convertAndSendToUser(
                    eq(fromUsername),
                    eq("/queue/errors"),
                    (WebRTCErrorResponse) argThat((WebRTCErrorResponse response) ->
                            response.type().equals("ERROR") &&
                                    response.error().code().equals(ErrorCode.ROOM_NOT_FOUND.getCode()) &&
                                    response.timestamp() != null
                    )
            );
        }

        @Test
        @DisplayName("CustomException 처리 - Principal이 null인 경우 메시지 전송 안 함")
        void t2() {
            // given
            CustomException exception = new CustomException(ErrorCode.ROOM_NOT_FOUND);

            // when
            controller.handleCustomException(exception, null);

            // then
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("일반 Exception 처리 - 사용자 큐로 내부 에러 전송")
        void t3() {
            // given
            Exception exception = new RuntimeException("예상치 못한 오류");

            // when
            controller.handleGeneralException(exception, authentication);

            // then
            verify(messagingTemplate).convertAndSendToUser(
                    eq(fromUsername),
                    eq("/queue/errors"),
                    (WebRTCErrorResponse) argThat((WebRTCErrorResponse response) ->
                            response.type().equals("ERROR") &&
                                    response.error().code().equals(ErrorCode.WS_INTERNAL_ERROR.getCode()) &&
                                    response.timestamp() != null
                    )
            );
        }

        @Test
        @DisplayName("일반 Exception 처리 - Principal이 null인 경우 메시지 전송 안 함")
        void t4() {
            // given
            Exception exception = new RuntimeException("예상치 못한 오류");

            // when
            controller.handleGeneralException(exception, null);

            // then
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
        }
    }
}