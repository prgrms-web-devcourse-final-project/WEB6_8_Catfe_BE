package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomRole;
import com.back.domain.studyroom.entity.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyRoomResponse {
    private Long roomId;
    private String title;
    private String description;
    private Boolean isPrivate;
    private String thumbnailUrl;
    private int currentParticipants;
    private int maxParticipants;
    private RoomStatus status;
    private RoomRole myRole;
    private LocalDateTime createdAt;
    
    public static MyRoomResponse of(Room room, long currentParticipants, RoomRole myRole) {
        return MyRoomResponse.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .description(room.getDescription() != null ? room.getDescription() : "")
                .isPrivate(room.isPrivate())  // 비공개 방 여부
                .thumbnailUrl(room.getThumbnailUrl())  // 썸네일 URL
                .currentParticipants((int) currentParticipants)  // Redis에서 조회한 실시간 값
                .maxParticipants(room.getMaxParticipants())
                .status(room.getStatus())
                .myRole(myRole)
                .createdAt(room.getCreatedAt())
                .build();
    }
}
