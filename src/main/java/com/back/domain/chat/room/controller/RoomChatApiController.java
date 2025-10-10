package com.back.domain.chat.room.controller;

import com.back.domain.chat.room.dto.ChatClearRequest;
import com.back.domain.chat.room.dto.ChatClearResponse;
import com.back.domain.chat.room.dto.ChatClearedNotification;
import com.back.domain.chat.room.dto.RoomChatPageResponse;
import com.back.domain.chat.room.service.RoomChatService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/messages")
@Tag(name = "RoomChat API", description = "스터디룸 채팅 메시지 관련 API")
public class RoomChatApiController {

    private final RoomChatService roomChatService;

    // 방 채팅 메시지 조회 (페이징, 특정 시간 이전 메시지)
    @GetMapping
    @Operation(summary = "스터디룸 채팅방 메시지 목록 조회", description = "특정 채팅방의 이전 메시지 기록을 페이징하여 조회합니다.")
    public ResponseEntity<RsData<RoomChatPageResponse>> getRoomChatMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        RoomChatPageResponse chatHistory = roomChatService.getRoomChatHistory(roomId, page, size, before);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("채팅 기록 조회 성공", chatHistory));
    }

    // 방 채팅 메시지 일괄 삭제 (방장, 부방장 권한)
    @DeleteMapping
    @Operation(
            summary = "스터디룸 채팅 일괄 삭제",
            description = "방장 또는 부방장이 해당 방의 모든 채팅 메시지를 삭제합니다. 실행 후 실시간으로 모든 방 멤버에게 알림이 전송됩니다."
    )
    public ResponseEntity<RsData<ChatClearResponse>> clearRoomMessages(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatClearRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 삭제 전 메시지 수 조회
        int messageCount = roomChatService.getRoomChatCount(roomId);

        // 채팅 일괄 삭제 실행
        ChatClearedNotification.ClearedByDto clearedByInfo =
                roomChatService.clearRoomChat(roomId, userDetails.getUserId());

        // 응답 데이터 생성
        ChatClearResponse responseData = ChatClearResponse.create(
                roomId,
                messageCount,
                clearedByInfo.userId(),
                clearedByInfo.nickname(),
                clearedByInfo.role()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("채팅 메시지 일괄 삭제 완료", responseData));
    }

}