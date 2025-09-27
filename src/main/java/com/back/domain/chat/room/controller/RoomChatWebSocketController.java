package com.back.domain.chat.room.controller;

import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.chat.room.dto.RoomChatMessageDto;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.dto.WebSocketErrorResponse;
import com.back.domain.chat.room.service.RoomChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Tag(name = "RoomChat WebSocket", description = "STOMP를 이용한 실시간 채팅 WebSocket 컨트롤러 (Swagger에서 직접 테스트 불가)")
public class RoomChatWebSocketController {

    private final RoomChatService roomChatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 방 채팅 메시지 처리
     * 클라이언트가 /app/chat/room/{roomId}로 메시지 전송 시 호출
     *
     * @param roomId 스터디룸 ID
     * @param chatMessage 채팅 메시지 (content, messageType, attachmentId)
     * @param headerAccessor WebSocket 헤더 정보
     * @param principal 인증된 사용자 정보
     */
    @MessageMapping("/chat/room/{roomId}")
    public void handleRoomChat(@DestinationVariable Long roomId,
                               RoomChatMessageDto chatMessage,
                               SimpMessageHeaderAccessor headerAccessor,
                               Principal principal) {

        try {
            // WebSocket에서 인증된 사용자 정보 추출
            CustomUserDetails userDetails = extractUserDetails(principal);
            if (userDetails == null) {
                sendErrorToUser(headerAccessor.getSessionId(), "WS_UNAUTHORIZED", "인증이 필요합니다");
                return;
            }

            Long currentUserId = userDetails.getUserId();
            String currentUserNickname = userDetails.getUsername();

            // 메시지 정보 보완
            RoomChatMessageDto enrichedMessage = chatMessage
                    .withRoomId(roomId)
                    .withUserId(currentUserId)
                    .withNickname(currentUserNickname);

            // DB에 메시지 저장
            RoomChatMessage savedMessage = roomChatService.saveRoomChatMessage(enrichedMessage);

            // 저장된 메시지 정보로 응답 DTO 생성
            RoomChatMessageDto responseMessage = RoomChatMessageDto.createResponse(
                    savedMessage.getId(),
                    roomId,
                    savedMessage.getUser().getId(),
                    savedMessage.getUser().getNickname(),
                    savedMessage.getUser().getProfileImageUrl(),
                    savedMessage.getContent(),
                    chatMessage.messageType(),
                    null, // 텍스트 채팅에서는 null
                    savedMessage.getCreatedAt()
            );

            // 해당 방의 모든 구독자에게 브로드캐스트
            messagingTemplate.convertAndSend("/topic/room/" + roomId, responseMessage);

        } catch (Exception e) {
            // 에러 응답을 해당 사용자에게만 전송
            WebSocketErrorResponse errorResponse = WebSocketErrorResponse.create(
                    "WS_ROOM_NOT_FOUND",
                    "존재하지 않는 방입니다"
            );

            // 에러를 발생시킨 사용자에게만 전송
            String sessionId = headerAccessor.getSessionId();
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", errorResponse);
        }
    }

    // WebSocket Principal에서 CustomUserDetails 추출
    private CustomUserDetails extractUserDetails(Principal principal) {
        if (principal instanceof Authentication auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof CustomUserDetails userDetails) {
                return userDetails;
            }
        }
        return null;
    }

    // 특정 사용자에게 에러 메시지 전송
    private void sendErrorToUser(String sessionId, String errorCode, String errorMessage) {
        WebSocketErrorResponse errorResponse = WebSocketErrorResponse.create(errorCode, errorMessage);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", errorResponse);
    }

}