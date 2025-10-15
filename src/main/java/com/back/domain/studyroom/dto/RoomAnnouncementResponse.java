package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomAnnouncement;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 방 공지사항 응답 DTO
 */
@Getter
@Builder
public class RoomAnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private Boolean isPinned;
    private LocalDateTime pinnedAt;
    private String createdByNickname;
    private Long createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static RoomAnnouncementResponse from(RoomAnnouncement announcement) {
        return RoomAnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .isPinned(announcement.isPinned())
                .pinnedAt(announcement.getPinnedAt())
                .createdByNickname(announcement.getCreatedBy().getNickname())
                .createdById(announcement.getCreatedBy().getId())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
