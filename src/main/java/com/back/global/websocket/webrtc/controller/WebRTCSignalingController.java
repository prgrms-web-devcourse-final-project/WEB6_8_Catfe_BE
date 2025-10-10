package com.back.global.websocket.webrtc.controller;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.CustomException;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.back.global.websocket.util.WebSocketAuthHelper;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaStateResponse;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaToggleRequest;
import com.back.global.websocket.webrtc.dto.signal.*;
import com.back.global.websocket.webrtc.service.WebRTCSignalValidator;
import com.back.global.websocket.util.WebSocketErrorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRTCSignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketErrorHelper errorHelper;
    private final WebRTCSignalValidator validator;
    private final WebSocketSessionManager sessionManager;

    // WebRTC Offer 메시지 처리
    @MessageMapping("/webrtc/offer")
    public void handleOffer(@Validated @Payload WebRTCOfferRequest request,
                            SimpMessageHeaderAccessor headerAccessor,
                            Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
            return;
        }

        Long fromUserId = userDetails.getUserId();
        Long targetUserId = request.targetUserId();

        log.info("WebRTC Offer 처리 시작 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);

        validator.validateSignal(request.roomId(), fromUserId, targetUserId);

        WebSocketSessionInfo targetSessionInfo = sessionManager.getSessionInfo(targetUserId);
        if (targetSessionInfo == null) {
            log.warn("WebRTC Offer 전송 실패 - 대상이 오프라인 상태입니다. User ID: {}", targetUserId);
            throw new CustomException(ErrorCode.WS_TARGET_OFFLINE);
        }

        WebRTCSignalResponse response = WebRTCSignalResponse.offerOrAnswer(
                WebRTCSignalType.OFFER, fromUserId, targetUserId, request.roomId(), request.sdp(), request.mediaType()
        );

        messagingTemplate.convertAndSendToUser(targetSessionInfo.sessionId(), "/queue/webrtc", response);
    }

    // WebRTC Answer 메시지 처리
    @MessageMapping("/webrtc/answer")
    public void handleAnswer(@Validated @Payload WebRTCAnswerRequest request,
                             SimpMessageHeaderAccessor headerAccessor,
                             Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
            return;
        }

        Long fromUserId = userDetails.getUserId();
        Long targetUserId = request.targetUserId();

        log.info("WebRTC Answer 처리 시작 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);

        validator.validateSignal(request.roomId(), fromUserId, targetUserId);

        WebSocketSessionInfo targetSessionInfo = sessionManager.getSessionInfo(targetUserId);
        if (targetSessionInfo == null) {
            log.warn("WebRTC Answer 전송 실패 - 대상이 오프라인 상태입니다. User ID: {}", targetUserId);
            throw new CustomException(ErrorCode.WS_TARGET_OFFLINE);
        }

        WebRTCSignalResponse response = WebRTCSignalResponse.offerOrAnswer(
                WebRTCSignalType.ANSWER, fromUserId, targetUserId, request.roomId(), request.sdp(), request.mediaType()
        );

        messagingTemplate.convertAndSendToUser(targetSessionInfo.sessionId(), "/queue/webrtc", response);
    }

    // ICE Candidate 메시지 처리
    @MessageMapping("/webrtc/ice-candidate")
    public void handleIceCandidate(@Validated @Payload WebRTCIceCandidateRequest request,
                                   SimpMessageHeaderAccessor headerAccessor,
                                   Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
            return;
        }

        Long fromUserId = userDetails.getUserId();
        Long targetUserId = request.targetUserId();

        log.debug("WebRTC ICE Candidate 처리 시작 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);

        validator.validateSignal(request.roomId(), fromUserId, targetUserId);

        WebSocketSessionInfo targetSessionInfo = sessionManager.getSessionInfo(targetUserId);
        if (targetSessionInfo == null) {
            return; // ICE Candidate는 실패해도 에러를 보내지 않고 조용히 무시
        }

        WebRTCSignalResponse response = WebRTCSignalResponse.iceCandidate(
                fromUserId, targetUserId, request.roomId(), request.candidate(), request.sdpMid(), request.sdpMLineIndex()
        );

        messagingTemplate.convertAndSendToUser(targetSessionInfo.sessionId(), "/queue/webrtc", response);
    }

    // 미디어 상태 토글 처리 (방 전체에 상태 공유)
    @MessageMapping("/webrtc/media/toggle")
    public void handleMediaToggle(@Validated @Payload WebRTCMediaToggleRequest request,
                                  SimpMessageHeaderAccessor headerAccessor,
                                  Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
            return;
        }

        Long userId = userDetails.getUserId();
        log.info("미디어 상태 변경 처리 시작 - Room: {}, User: {}", request.roomId(), userId);

        validator.validateMediaStateChange(request.roomId(), userId);

        WebRTCMediaStateResponse response = WebRTCMediaStateResponse.of(
                userId, userDetails.getUsername(), request.mediaType(), request.enabled()
        );

        messagingTemplate.convertAndSend("/topic/room/" + request.roomId() + "/media-status", response);
    }

    // WebRTC 시그널링 처리 중 발생하는 CustomException 처리
    @MessageExceptionHandler(CustomException.class)
    public void handleCustomException(CustomException e, SimpMessageHeaderAccessor headerAccessor) {
        log.warn("WebRTC 시그널링 오류 발생: {}", e.getMessage());
        errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);
    }

    // 예상치 못한 모든 Exception 처리
    @MessageExceptionHandler(Exception.class)
    public void handleGeneralException(Exception e, SimpMessageHeaderAccessor headerAccessor) {
        log.error("WebRTC 시그널링 처리 중 예상치 못한 오류 발생", e);
        errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "시그널링 처리 중 오류가 발생했습니다.");
    }
}