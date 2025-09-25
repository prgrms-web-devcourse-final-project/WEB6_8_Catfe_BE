package com.back.domain.chat.controller;

import com.back.domain.chat.dto.ChatPageResponse;
import com.back.domain.chat.service.ChatService;
import com.back.global.common.dto.RsData;
import com.back.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Chat API", description = "채팅 메시지 조회 관련 API")
public class ChatApiController {

    private final ChatService chatService;

    // 방 채팅 메시지 조회 (페이징, 특정 시간 이전 메시지)
    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "채팅방 메시지 목록 조회", description = "특정 채팅방의 이전 메시지 기록을 페이징하여 조회합니다.")
    public ResponseEntity<RsData<ChatPageResponse>> getRoomChatMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ChatPageResponse chatHistory = chatService.getRoomChatHistory(roomId, page, size, before);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("채팅 기록 조회 성공", chatHistory));
    }

}