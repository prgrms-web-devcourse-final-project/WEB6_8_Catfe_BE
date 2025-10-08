package com.back.domain.studyroom.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 멤버 퇴장 알림 DTO
 * WebSocket을 통해 방 참가자들에게 브로드캐스트
 */
@Getter
@Builder
public class MemberLeftNotification {
    private Long roomId;
    private Long userId;
    private String nickname;
    private LocalDateTime timestamp;
    
    public static MemberLeftNotification of(Long roomId, Long userId, String nickname) {
        return MemberLeftNotification.builder()
                .roomId(roomId)
                .userId(userId)
                .nickname(nickname)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
