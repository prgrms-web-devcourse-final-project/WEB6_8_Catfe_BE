package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 역할 변경 WebSocket 알림 DTO
 * - 방 멤버의 역할이 변경되었을 때 실시간 브로드캐스트
 */
@Getter
@Builder
public class RoleChangedNotification {
    
    private Long roomId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private RoomRole oldRole;
    private RoomRole newRole;
    private String message;
    private LocalDateTime timestamp;
    
    public static RoleChangedNotification of(
            Long roomId,
            Long userId,
            String nickname,
            String profileImageUrl,
            RoomRole oldRole,
            RoomRole newRole) {
        
        String message = buildMessage(nickname, oldRole, newRole);
        
        return RoleChangedNotification.builder()
                .roomId(roomId)
                .userId(userId)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .oldRole(oldRole)
                .newRole(newRole)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    private static String buildMessage(String nickname, RoomRole oldRole, RoomRole newRole) {
        if (newRole == RoomRole.HOST) {
            return String.format("%s님이 방장으로 임명되었습니다.", nickname);
        } else if (oldRole == RoomRole.HOST) {
            return String.format("%s님이 일반 멤버로 변경되었습니다.", nickname);
        } else if (newRole == RoomRole.SUB_HOST) {
            return String.format("%s님이 부방장으로 승격되었습니다.", nickname);
        } else if (newRole == RoomRole.MEMBER && oldRole == RoomRole.VISITOR) {
            return String.format("%s님이 정식 멤버로 승격되었습니다.", nickname);
        } else if (newRole == RoomRole.MEMBER) {
            return String.format("%s님이 일반 멤버로 변경되었습니다.", nickname);
        }
        return String.format("%s님의 역할이 변경되었습니다.", nickname);
    }
}
