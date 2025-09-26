package com.back.domain.chat.dm.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PrivateMessageHistoryResponse(
        List<PrivateMessageDto> content,
        PageableDto pageable
) {

    // 개인 메시지 정보 DTO
    public record PrivateMessageDto(
            Long messageId,
            Long fromUserId,
            Long toUserId,
            String content,
            String messageType,
            PrivateMessageResponse.AttachmentDto attachment,
            LocalDateTime createdAt,
            Boolean isRead
    ) {}

    // 페이징 정보 DTO
    public record PageableDto(
            Integer page,
            Integer size,
            Boolean hasNext
    ) {}
}
