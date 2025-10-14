package com.back.global.websocket.webrtc.controller;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.CustomException;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.util.WebSocketAuthHelper;
import com.back.global.websocket.webrtc.dto.WebRTCErrorResponse;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaStateResponse;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaToggleRequest;
import com.back.global.websocket.webrtc.dto.signal.*;
import com.back.global.websocket.webrtc.service.WebRTCSignalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRTCSignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebRTCSignalValidator validator;

    // WebRTC Offer 메시지 처리
    @MessageMapping("/webrtc/offer")
    public void handleOffer(@Validated @Payload WebRTCOfferRequest request, Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long fromUserId = userDetails.getUserId();
        Long targetUserId = request.targetUserId();

        log.info("[WebRTC] Offer 처리 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);

        // 같은 방에 있는지, 자기 자신에게 보내는 건 아닌지 검증
        validator.validateSignal(request.roomId(), fromUserId, targetUserId);

        // Offer 응답 생성
        WebRTCSignalResponse response = WebRTCSignalResponse.offerOrAnswer(
                WebRTCSignalType.OFFER,
                fromUserId,
                targetUserId,
                request.roomId(),
                request.sdp(),
                request.mediaType()
        );

        // 방 토픽으로 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/room/" + request.roomId() + "/webrtc",
                response
        );

        log.debug("[WebRTC] Offer 전송 완료 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);
    }

    // WebRTC Answer 메시지 처리
    @MessageMapping("/webrtc/answer")
    public void handleAnswer(@Validated @Payload WebRTCAnswerRequest request, Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long fromUserId = userDetails.getUserId();
        Long targetUserId = request.targetUserId();

        log.info("[WebRTC] Answer 처리 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);

        // 같은 방에 있는지, 자기 자신에게 보내는 건 아닌지 검증
        validator.validateSignal(request.roomId(), fromUserId, targetUserId);

        // Answer 응답 생성 (targetUserId 포함)
        WebRTCSignalResponse response = WebRTCSignalResponse.offerOrAnswer(
                WebRTCSignalType.ANSWER,
                fromUserId,
                targetUserId,
                request.roomId(),
                request.sdp(),
                request.mediaType()
        );

        // 방 토픽으로 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/room/" + request.roomId() + "/webrtc",
                response
        );

        log.debug("[WebRTC] Answer 전송 완료 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);
    }

    // ICE Candidate 메시지 처리
    @MessageMapping("/webrtc/ice-candidate")
    public void handleIceCandidate(@Validated @Payload WebRTCIceCandidateRequest request, Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long fromUserId = userDetails.getUserId();
        Long targetUserId = request.targetUserId();

        log.debug("[WebRTC] ICE Candidate 처리 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);

        // 같은 방에 있는지, 자기 자신에게 보내는 건 아닌지 검증
        validator.validateSignal(request.roomId(), fromUserId, targetUserId);

        // ICE Candidate 응답 생성
        WebRTCSignalResponse response = WebRTCSignalResponse.iceCandidate(
                fromUserId,
                targetUserId,
                request.roomId(),
                request.candidate(),
                request.sdpMid(),
                request.sdpMLineIndex()
        );

        // 방 토픽으로 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/room/" + request.roomId() + "/webrtc",
                response
        );
    }

    /**
     * 미디어 상태 토글 처리
     * 방 전체에 미디어 상태 변경 브로드캐스트
     */
    @MessageMapping("/webrtc/media/toggle")
    public void handleMediaToggle(@Validated @Payload WebRTCMediaToggleRequest request, Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = userDetails.getUserId();

        log.info("[WebRTC] 미디어 상태 변경 - Room: {}, User: {}, Type: {}, Enabled: {}",
                request.roomId(), userId, request.mediaType(), request.enabled());

        // 방 멤버인지 검증
        validator.validateMediaStateChange(request.roomId(), userId);

        // 미디어 상태 응답 생성
        WebRTCMediaStateResponse response = WebRTCMediaStateResponse.of(
                userId,
                userDetails.getUsername(),
                request.mediaType(),
                request.enabled()
        );

        // 방 전체에 미디어 상태 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/room/" + request.roomId() + "/media-status",
                response
        );
    }

    // WebRTC 시그널링 처리 중 발생하는 CustomException 처리
    @MessageExceptionHandler(CustomException.class)
    public void handleCustomException(CustomException e, Principal principal) {
        if (principal == null) {
            log.warn("[WebRTC] 인증 정보 없는 사용자의 오류: {}", e.getMessage());
            return;
        }

        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            log.warn("[WebRTC] Principal에서 사용자 정보 추출 실패");
            return;
        }

        log.warn("[WebRTC] 시그널링 오류 - User: {} (ID: {}), Error: {}",
                principal.getName(), userDetails.getUserId(), e.getMessage());

        // 에러는 개인 큐로 전송 (방 전체에 보낼 필요 없음)
        WebRTCErrorResponse errorResponse = WebRTCErrorResponse.from(e);

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",  // 에러 전용 큐
                errorResponse
        );
    }

    // 예상치 못한 모든 Exception 처리
    @MessageExceptionHandler(Exception.class)
    public void handleGeneralException(Exception e, Principal principal) {
        if (principal == null) {
            log.error("[WebRTC] 인증 정보 없는 사용자의 예외 발생", e);
            return;
        }

        log.error("[WebRTC] 예상치 못한 오류 발생 - User: {}", principal.getName(), e);

        CustomException customException = new CustomException(ErrorCode.WS_INTERNAL_ERROR);
        WebRTCErrorResponse errorResponse = WebRTCErrorResponse.from(customException);

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                errorResponse
        );
    }
}