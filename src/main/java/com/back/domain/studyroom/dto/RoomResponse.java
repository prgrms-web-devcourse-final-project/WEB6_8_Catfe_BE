package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomResponse {
    private Long roomId;
    private String title;
    private String description;
    private int currentParticipants;
    private int maxParticipants;
    private RoomStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    
    public static RoomResponse from(Room room, long currentParticipants) {
        return RoomResponse.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .description(room.getDescription() != null ? room.getDescription() : "")
                .currentParticipants((int) currentParticipants)  // Redis에서 조회한 실시간 값
                .maxParticipants(room.getMaxParticipants())
                .status(room.getStatus())
                .createdBy(room.getCreatedBy().getNickname())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
