package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 멤버 입장 알림 DTO
 * WebSocket을 통해 방 참가자들에게 브로드캐스트
 */
@Getter
@Builder
public class MemberJoinedNotification {
    private Long roomId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private RoomRole role;  // 입장 시 역할 (보통 VISITOR)
    private LocalDateTime timestamp;
    
    public static MemberJoinedNotification of(Long roomId, Long userId, String nickname, 
                                             String profileImageUrl, RoomRole role) {
        return MemberJoinedNotification.builder()
                .roomId(roomId)
                .userId(userId)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .role(role)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
