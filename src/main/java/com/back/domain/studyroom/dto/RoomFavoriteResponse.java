package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 즐겨찾기한 방 응답 DTO
 */
@Getter
@Builder
public class RoomFavoriteResponse {
    private Long roomId;
    private String title;
    private String description;
    private Boolean isPrivate;
    private String thumbnailUrl;
    private Integer currentParticipants;
    private Integer maxParticipants;
    private RoomStatus status;
    private String createdBy;
    private LocalDateTime favoritedAt;
    
    public static RoomFavoriteResponse of(Room room, long currentParticipants, LocalDateTime favoritedAt) {
        return RoomFavoriteResponse.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .description(room.getDescription())
                .isPrivate(room.isPrivate())
                .thumbnailUrl(room.getThumbnailUrl())
                .currentParticipants((int) currentParticipants)
                .maxParticipants(room.getMaxParticipants())
                .status(room.getStatus())
                .createdBy(room.getCreatedBy().getNickname())
                .favoritedAt(favoritedAt)
                .build();
    }
}
