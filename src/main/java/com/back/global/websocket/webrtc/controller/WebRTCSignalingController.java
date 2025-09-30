package com.back.global.websocket.webrtc.controller;

import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaToggleRequest;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaStateResponse;
import com.back.global.websocket.webrtc.dto.signal.*;
import com.back.global.websocket.webrtc.service.WebRTCSignalValidator;
import com.back.global.websocket.util.WebSocketErrorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
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

    // WebRTC Offer 메시지 처리
    @MessageMapping("/webrtc/offer")
    public void handleOffer(@Validated @Payload WebRTCOfferRequest request,
                            SimpMessageHeaderAccessor headerAccessor,
                            Principal principal) {
        try {
            // WebSocket에서 인증된 사용자 정보 추출
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
                return;
            }

            Long fromUserId = userDetails.getUserId();

            // 시그널 검증
            validator.validateSignal(request.roomId(), fromUserId, request.targetUserId());

            log.info("WebRTC Offer received - Room: {}, From: {}, To: {}, MediaType: {}",
                    request.roomId(), fromUserId, request.targetUserId(), request.mediaType());

            // Offer 메시지 생성
            WebRTCSignalResponse response = WebRTCSignalResponse.offerOrAnswer(
                    WebRTCSignalType.OFFER,
                    fromUserId,
                    request.targetUserId(),
                    request.roomId(),
                    request.sdp(),
                    request.mediaType()
            );

            // 방 전체에 브로드캐스트 (P2P Mesh 연결)
            messagingTemplate.convertAndSend(
                    "/topic/room/" + request.roomId() + "/webrtc",
                    response
            );

        } catch (Exception e) {
            log.error("WebRTC Offer 처리 중 오류 발생 - roomId: {}", request.roomId(), e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "Offer 전송 중 오류가 발생했습니다");
        }
    }

    // WebRTC Answer 메시지 처리
    @MessageMapping("/webrtc/answer")
    public void handleAnswer(@Validated @Payload WebRTCAnswerRequest request,
                             SimpMessageHeaderAccessor headerAccessor,
                             Principal principal) {
        try {
            // WebSocket에서 인증된 사용자 정보 추출
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
                return;
            }

            Long fromUserId = userDetails.getUserId();

            // 시그널 검증
            validator.validateSignal(request.roomId(), fromUserId, request.targetUserId());

            log.info("WebRTC Answer received - Room: {}, From: {}, To: {}, MediaType: {}",
                    request.roomId(), fromUserId, request.targetUserId(), request.mediaType());

            // Answer 메시지 생성
            WebRTCSignalResponse response = WebRTCSignalResponse.offerOrAnswer(
                    WebRTCSignalType.ANSWER,
                    fromUserId,
                    request.targetUserId(),
                    request.roomId(),
                    request.sdp(),
                    request.mediaType()
            );

            // 방 전체에 브로드캐스트 (P2P Mesh 연결)
            messagingTemplate.convertAndSend(
                    "/topic/room/" + request.roomId() + "/webrtc",
                    response
            );

        } catch (Exception e) {
            log.error("WebRTC Answer 처리 중 오류 발생 - roomId: {}", request.roomId(), e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "Answer 전송 중 오류가 발생했습니다");
        }
    }

    // ICE Candidate 메시지 처리
    @MessageMapping("/webrtc/ice-candidate")
    public void handleIceCandidate(@Validated @Payload WebRTCIceCandidateRequest request,
                                   SimpMessageHeaderAccessor headerAccessor,
                                   Principal principal) {
        try {
            // WebSocket에서 인증된 사용자 정보 추출
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
                return;
            }

            Long fromUserId = userDetails.getUserId();

            // 시그널 검증
            validator.validateSignal(request.roomId(), fromUserId, request.targetUserId());

            log.info("ICE Candidate received - Room: {}, From: {}, To: {}",
                    request.roomId(), fromUserId, request.targetUserId());

            // ICE Candidate 메시지 생성
            WebRTCSignalResponse response = WebRTCSignalResponse.iceCandidate(
                    fromUserId,
                    request.targetUserId(),
                    request.roomId(),
                    request.candidate(),
                    request.sdpMid(),
                    request.sdpMLineIndex()
            );

            // 방 전체에 브로드캐스트 (P2P Mesh 연결)
            messagingTemplate.convertAndSend(
                    "/topic/room/" + request.roomId() + "/webrtc",
                    response
            );

        } catch (Exception e) {
            log.error("ICE Candidate 처리 중 오류 발생 - roomId: {}", request.roomId(), e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "ICE Candidate 전송 중 오류가 발생했습니다");
        }
    }

    // 미디어 상태 토글 처리
    @MessageMapping("/webrtc/media/toggle")
    public void handleMediaToggle(@Validated @Payload WebRTCMediaToggleRequest request,
                                  SimpMessageHeaderAccessor headerAccessor,
                                  Principal principal) {
        try {
            // 인증된 사용자 정보 추출
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
                return;
            }

            Long userId = userDetails.getUserId();
            String nickname = userDetails.getUsername();

            // 미디어 상태 변경 검증
            validator.validateMediaStateChange(request.roomId(), userId);

            log.info("미디어 상태 변경 - Room: {}, User: {}, MediaType: {}, Enabled: {}",
                    request.roomId(), userId, request.mediaType(), request.enabled());

            // 미디어 상태 응답 생성
            WebRTCMediaStateResponse response = WebRTCMediaStateResponse.of(
                    userId,
                    nickname,
                    request.mediaType(),
                    request.enabled()
            );

            // 방 전체에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/room/" + request.roomId() + "/media-status",
                    response
            );

        } catch (Exception e) {
            log.error("미디어 상태 변경 중 오류 발생 - roomId: {}", request.roomId(), e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "미디어 상태 변경 중 오류가 발생했습니다");
        }
    }

    // Principal에서 CustomUserDetails 추출 헬퍼 메서드
    private CustomUserDetails extractUserDetails(Principal principal) {
        if (principal instanceof Authentication auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof CustomUserDetails userDetails) {
                return userDetails;
            }
        }
        return null;
    }
}