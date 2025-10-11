package com.back.domain.chat.room.controller;

import com.back.domain.chat.room.dto.RoomChatMessageRequest;
import com.back.domain.chat.room.dto.RoomChatMessageResponse;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import com.back.domain.chat.room.service.RoomChatService;
import com.back.global.websocket.util.WebSocketAuthHelper;
import com.back.global.websocket.util.WebSocketErrorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomChatWebSocketController {

    private final RoomChatService roomChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketErrorHelper errorHelper;

    /**
     * 방 채팅 메시지 처리
     * 클라이언트가 /app/chat/room/{roomId}로 메시지 전송 시 호출
     */
    @MessageMapping("/chat/room/{roomId}")
    public void handleRoomChat(@DestinationVariable Long roomId,
                               @Payload RoomChatMessageRequest request,
                               Principal principal) {

        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            // 인증 정보가 없는 경우 예외 처리
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        RoomChatMessage savedMessage = roomChatService.saveRoomChatMessage(
                roomId,
                userDetails.getUserId(),
                request
        );

        // 응답 DTO 생성 및 브로드캐스트
        RoomChatMessageResponse responseMessage = RoomChatMessageResponse.from(savedMessage);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, responseMessage);
    }

    // 채팅 메시지 처리 중 발생하는 예외 중앙 처리
    @MessageExceptionHandler(CustomException.class)
    public void handleChatException(CustomException e, SimpMessageHeaderAccessor headerAccessor) {
        log.warn("채팅 메시지 처리 실패 - SessionId: {}, Error: {}", headerAccessor.getSessionId(), e.getMessage());
        errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);
    }
}