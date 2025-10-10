package com.back.global.websocket.webrtc.controller;

import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaStateResponse;
import com.back.global.websocket.webrtc.dto.media.WebRTCMediaToggleRequest;
import com.back.global.websocket.webrtc.dto.signal.*;
import com.back.global.websocket.webrtc.service.WebRTCSignalValidator;
import com.back.global.websocket.service.WebSocketSessionManager;
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
    private final WebSocketSessionManager sessionManager;

    // WebRTC Offer 메시지 처리
    @MessageMapping("/webrtc/offer")
    public void handleOffer(@Validated @Payload WebRTCOfferRequest request,
                            SimpMessageHeaderAccessor headerAccessor,
                            Principal principal) {
        try {
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
                return;
            }

            Long fromUserId = userDetails.getUserId();
            Long targetUserId = request.targetUserId();

            // 시그널 검증
            validator.validateSignal(request.roomId(), fromUserId, targetUserId);

            // 상대방의 온라인 세션 정보 조회
            WebSocketSessionInfo targetSessionInfo = sessionManager.getSessionInfo(targetUserId);
            if (targetSessionInfo == null) {
                log.warn("WebRTC Offer 전송 실패 - 대상이 오프라인 상태입니다. User ID: {}", targetUserId);
                errorHelper.sendErrorToUser(headerAccessor.getSessionId(), "WEBRTC_TARGET_OFFLINE", "상대방이 오프라인 상태입니다.");
                return;
            }

            log.info("WebRTC Offer received - Room: {}, From: {}, To: {}",
                    request.roomId(), fromUserId, targetUserId);

            // 응답 메시지 생성
            WebRTCSignalResponse response = WebRTCSignalResponse.offerOrAnswer(
                    WebRTCSignalType.OFFER,
                    fromUserId,
                    targetUserId,
                    request.roomId(),
                    request.sdp(),
                    request.mediaType()
            );

            // 특정 사용자에게만 1:1로 메시지 전송
            messagingTemplate.convertAndSendToUser(
                    targetSessionInfo.sessionId(),
                    "/queue/webrtc",
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
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
                return;
            }

            Long fromUserId = userDetails.getUserId();
            Long targetUserId = request.targetUserId();
            validator.validateSignal(request.roomId(), fromUserId, targetUserId);

            WebSocketSessionInfo targetSessionInfo = sessionManager.getSessionInfo(targetUserId);
            if (targetSessionInfo == null) {
                log.warn("WebRTC Answer 전송 실패 - 대상이 오프라인 상태입니다. User ID: {}", targetUserId);
                errorHelper.sendErrorToUser(headerAccessor.getSessionId(), "WEBRTC_TARGET_OFFLINE", "상대방이 오프라인 상태입니다.");
                return;
            }

            log.info("WebRTC Answer received - Room: {}, From: {}, To: {}",
                    request.roomId(), fromUserId, targetUserId);

            WebRTCSignalResponse response = WebRTCSignalResponse.offerOrAnswer(
                    WebRTCSignalType.ANSWER,
                    fromUserId,
                    targetUserId,
                    request.roomId(),
                    request.sdp(),
                    request.mediaType()
            );

            // 특정 사용자에게만 1:1로 메시지 전송
            messagingTemplate.convertAndSendToUser(
                    targetSessionInfo.sessionId(),
                    "/queue/webrtc",
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
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
                return;
            }

            Long fromUserId = userDetails.getUserId();
            Long targetUserId = request.targetUserId();
            validator.validateSignal(request.roomId(), fromUserId, targetUserId);

            WebSocketSessionInfo targetSessionInfo = sessionManager.getSessionInfo(targetUserId);
            if (targetSessionInfo == null) {
                log.warn("ICE Candidate 전송 실패 - 대상이 오프라인 상태입니다. User ID: {}", targetUserId);
                // ICE Candidate는 연결 과정에서 다수 발생하므로 상대가 없어도 에러 전송은 생략 가능
                return;
            }

            log.info("ICE Candidate received - Room: {}, From: {}, To: {}",
                    request.roomId(), fromUserId, targetUserId);

            WebRTCSignalResponse response = WebRTCSignalResponse.iceCandidate(
                    fromUserId,
                    targetUserId,
                    request.roomId(),
                    request.candidate(),
                    request.sdpMid(),
                    request.sdpMLineIndex()
            );

            // 특정 사용자에게만 1:1로 메시지 전송
            messagingTemplate.convertAndSendToUser(
                    targetSessionInfo.sessionId(),
                    "/queue/webrtc",
                    response
            );

        } catch (Exception e) {
            log.error("ICE Candidate 처리 중 오류 발생 - roomId: {}", request.roomId(), e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "ICE Candidate 전송 중 오류가 발생했습니다");
        }
    }

    // 미디어 상태 토글 처리 (방 전체에 상태 공유)
    @MessageMapping("/webrtc/media/toggle")
    public void handleMediaToggle(@Validated @Payload WebRTCMediaToggleRequest request,
                                  SimpMessageHeaderAccessor headerAccessor,
                                  Principal principal) {
        try {
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
                return;
            }

            Long userId = userDetails.getUserId();
            validator.validateMediaStateChange(request.roomId(), userId);

            log.info("미디어 상태 변경 - Room: {}, User: {}, MediaType: {}, Enabled: {}",
                    request.roomId(), userId, request.mediaType(), request.enabled());

            WebRTCMediaStateResponse response = WebRTCMediaStateResponse.of(
                    userId,
                    userDetails.getUsername(),
                    request.mediaType(),
                    request.enabled()
            );

            // 미디어 상태는 방 전체에 브로드캐스트
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
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }
}