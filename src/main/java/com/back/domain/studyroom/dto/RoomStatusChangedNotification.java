package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 방 상태 변경 알림 DTO
 * WebSocket을 통해 방 참가자들에게 브로드캐스트
 */
@Getter
@Builder
public class RoomStatusChangedNotification {
    private Long roomId;
    private RoomStatus oldStatus;
    private RoomStatus newStatus;
    private String changedBy;  // 변경한 사용자 닉네임
    private LocalDateTime timestamp;
    
    public static RoomStatusChangedNotification of(Long roomId, RoomStatus oldStatus, 
                                                   RoomStatus newStatus, String changedBy) {
        return RoomStatusChangedNotification.builder()
                .roomId(roomId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
