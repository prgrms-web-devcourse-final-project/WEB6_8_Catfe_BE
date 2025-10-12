package com.back.global.websocket.webrtc.controller;

import com.back.global.exception.ErrorCode;
import com.back.global.exception.CustomException;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.back.global.websocket.util.WebSocketAuthHelper;
import com.back.global.websocket.webrtc.dto.WebRTCErrorResponse;
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
    public void handleOffer(@Validated @Payload WebRTCOfferRequest request, Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
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

        messagingTemplate.convertAndSendToUser(targetSessionInfo.username(), "/queue/webrtc", response);
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

        messagingTemplate.convertAndSendToUser(targetSessionInfo.username(), "/queue/webrtc", response);
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

        log.debug("WebRTC ICE Candidate 처리 시작 - Room: {}, From: {}, To: {}", request.roomId(), fromUserId, targetUserId);

        validator.validateSignal(request.roomId(), fromUserId, targetUserId);

        WebSocketSessionInfo targetSessionInfo = sessionManager.getSessionInfo(targetUserId);
        if (targetSessionInfo == null) {
            return; // ICE Candidate는 실패해도 에러를 보내지 않고 조용히 무시
        }

        WebRTCSignalResponse response = WebRTCSignalResponse.iceCandidate(
                fromUserId, targetUserId, request.roomId(), request.candidate(), request.sdpMid(), request.sdpMLineIndex()
        );

        messagingTemplate.convertAndSendToUser(targetSessionInfo.username(), "/queue/webrtc", response);
    }

    // 미디어 상태 토글 처리 (방 전체에 상태 공유)
    @MessageMapping("/webrtc/media/toggle")
    public void handleMediaToggle(@Validated @Payload WebRTCMediaToggleRequest request, Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
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
    public void handleCustomException(CustomException e, Principal principal) {
        if (principal == null) {
            log.warn("인증 정보 없는 사용자의 WebRTC 오류: {}", e.getMessage());
            return;
        }

        log.warn("WebRTC 시그널링 오류 발생 (to {}): {}", principal.getName(), e.getMessage());

        WebRTCErrorResponse errorResponse = WebRTCErrorResponse.from(e);

        messagingTemplate.convertAndSendToUser(
                principal.getName(), // 에러를 발생시킨 사람의 username
                "/queue/webrtc",
                errorResponse
        );
    }

    // 예상치 못한 모든 Exception 처리
    @MessageExceptionHandler(Exception.class)
    public void handleGeneralException(Exception e, Principal principal) { // headerAccessor -> Principal
        if (principal == null) {
            log.error("WebRTC 처리 중 인증 정보 없는 사용자의 예외 발생", e);
            return;
        }

        log.error("WebRTC 시그널링 처리 중 예상치 못한 오류 발생 (to {})", principal.getName(), e);

        // CustomException으로 감싸서 일관된 형식의 에러 DTO를 생성
        CustomException customException = new CustomException(ErrorCode.WS_INTERNAL_ERROR);
        WebRTCErrorResponse errorResponse = WebRTCErrorResponse.from(customException);

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/webrtc",
                errorResponse
        );
    }
}