package com.back.domain.chat.room.dto;

/**
 * WebSocket 브로드캐스트용 채팅 삭제 알림 DTO
 */
public record ChatClearedNotification(
        String type,
        Long roomId,
        java.time.LocalDateTime clearedAt,
        ClearedByDto clearedBy,
        Integer deletedCount,
        String message
) {

    public record ClearedByDto(
            Long userId,
            String nickname,
            String profileImageUrl,
            String role
    ) {}

    // 알림 생성 헬퍼
    public static ChatClearedNotification create(Long roomId, int deletedCount,
                                                 Long userId, String nickname, String profileImageUrl, String role) {
        ClearedByDto clearedBy = new ClearedByDto(userId, nickname, profileImageUrl, role);
        String message = nickname + "님이 모든 채팅을 삭제했습니다.";

        return new ChatClearedNotification(
                "CHAT_CLEARED",
                roomId,
                java.time.LocalDateTime.now(),
                clearedBy,
                deletedCount,
                message
        );
    }
}
