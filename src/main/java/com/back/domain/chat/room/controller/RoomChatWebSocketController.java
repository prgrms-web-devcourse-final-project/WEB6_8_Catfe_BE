package com.back.domain.chat.room.controller;

import com.back.domain.chat.room.dto.ChatClearRequest;
import com.back.domain.chat.room.dto.ChatClearedNotification;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.chat.room.dto.RoomChatMessageDto;
import com.back.global.exception.CustomException;
import com.back.global.security.user.CustomUserDetails;
import com.back.domain.chat.room.service.RoomChatService;
import com.back.global.websocket.util.WebSocketAuthHelper;
import com.back.global.websocket.util.WebSocketErrorHelper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "RoomChat WebSocket", description = "STOMP를 이용한 실시간 채팅 WebSocket 컨트롤러 (Swagger에서 직접 테스트 불가)")
public class RoomChatWebSocketController {

    private final RoomChatService roomChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketAuthHelper authHelper;
    private final WebSocketErrorHelper errorHelper;

    /**
     * 방 채팅 메시지 처리
     * 클라이언트가 /app/chat/room/{roomId}로 메시지 전송 시 호출
     */
    @MessageMapping("/chat/room/{roomId}")
    public void handleRoomChat(@DestinationVariable Long roomId,
                               RoomChatMessageDto chatMessage,
                               SimpMessageHeaderAccessor headerAccessor,
                               Principal principal) {

        try {
            // WebSocket에서 인증된 사용자 정보 추출
            CustomUserDetails userDetails = authHelper.extractUserDetails(principal);

            if (userDetails == null) {
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
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

        } catch (CustomException e) {
            log.warn("채팅 메시지 처리 실패 - roomId: {}, error: {}", roomId, e.getMessage());
            errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);

        } catch (Exception e) {
            log.error("채팅 메시지 처리 중 예상치 못한 오류 발생 - roomId: {}", roomId, e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "메시지 전송 중 오류가 발생했습니다");
        }
    }

}