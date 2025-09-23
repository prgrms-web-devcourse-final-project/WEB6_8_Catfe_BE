package com.back.domain.websocket.controller;

import com.back.domain.websocket.dto.ChatPageResponse;
import com.back.domain.websocket.service.ChatService;
import com.back.global.common.dto.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;

    // 방 채팅 메시지 조회 (페이징, 특정 시간 이전 메시지)
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<RsData<ChatPageResponse>> getRoomChatMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestHeader("Authorization") String authorization) {

        // size 최대값 제한 (임시: max 100)
        if (size > 100) {
            size = 100;
        }

        // TODO: JWT 토큰에서 사용자 정보 추출 및 권한 확인

        ChatPageResponse chatHistory = chatService.getRoomChatHistory(roomId, page, size, before);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("채팅 기록 조회 성공", chatHistory));
    }

    // 방 채팅 메시지 삭제
    @DeleteMapping("/rooms/{roomId}/messages/{messageId}")
    public ResponseEntity<RsData<Map<String, Object>>> deleteRoomMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestHeader("Authorization") String authorization) {

        // TODO: JWT 토큰에서 사용자 정보 추출

        // 임시로 하드코딩 (테스트용)
        Long currentUserId = 1L;

        // 메시지 삭제 로직 실행
        chatService.deleteRoomMessage(roomId, messageId, currentUserId);

        Map<String, Object> responseData = Map.of(
                "messageId", messageId,
                "deletedAt", LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("메시지 삭제 성공", responseData));
    }
}