package com.back.global.websocket.webrtc.controller;

import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.util.WebSocketErrorHelper;
import com.back.global.websocket.webrtc.dto.*;
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

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebRTCSignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketErrorHelper errorHelper;

    /**
     * WebRTC Offer 메시지 처리
     * P2P Mesh 방식: 방 전체에 브로드캐스트하여 모든 참가자가 수신
     * 클라이언트가 /app/webrtc/offer로 전송
     */
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

    /**
     * WebRTC Answer 메시지 처리
     * P2P Mesh 방식: 방 전체에 브로드캐스트하여 모든 참가자가 수신
     * 클라이언트가 /app/webrtc/answer로 전송
     */
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

            // 방 전체에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/room/" + request.roomId() + "/webrtc",
                    response
            );

        } catch (Exception e) {
            log.error("WebRTC Answer 처리 중 오류 발생 - roomId: {}", request.roomId(), e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "Answer 전송 중 오류가 발생했습니다");
        }
    }

    /**
     * ICE Candidate 메시지 처리
     * P2P Mesh 방식: 방 전체에 브로드캐스트하여 모든 참가자가 수신
     * 클라이언트가 /app/webrtc/ice-candidate로 전송
     */
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

            // 방 전체에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/room/" + request.roomId() + "/webrtc",
                    response
            );

        } catch (Exception e) {
            log.error("ICE Candidate 처리 중 오류 발생 - roomId: {}", request.roomId(), e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "ICE Candidate 전송 중 오류가 발생했습니다");
        }
    }

    /**
     * WebSocket Principal에서 CustomUserDetails 추출
     */
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