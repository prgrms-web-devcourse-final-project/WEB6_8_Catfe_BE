package com.back.domain.chat.room.dto;

public record ChatClearResponse(
        Long roomId,
        Integer deletedCount,
        java.time.LocalDateTime clearedAt,
        ClearedByDto clearedBy
) {

    public record ClearedByDto(
            Long userId,
            String nickname,
            String role
    ) {}

    // 성공 응답 생성 헬퍼
    public static ChatClearResponse create(Long roomId, int deletedCount,
                                           Long userId, String nickname, String role) {
        return new ChatClearResponse(
                roomId,
                deletedCount,
                java.time.LocalDateTime.now(),
                new ClearedByDto(userId, nickname, role)
        );
    }
}
