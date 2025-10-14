package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RoomDetailResponse {
    private Long roomId;
    private String title;
    private String description;
    private boolean isPrivate;
    private String thumbnailUrl;  // 썸네일 이미지 URL
    private int maxParticipants;
    private int currentParticipants;
    private RoomStatus status;
    private boolean allowCamera;
    private boolean allowAudio;
    private boolean allowScreenShare;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<RoomMemberResponse> members;
    
    public static RoomDetailResponse of(Room room, long currentParticipants, List<RoomMemberResponse> members) {
        return RoomDetailResponse.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .description(room.getDescription() != null ? room.getDescription() : "")
                .isPrivate(room.isPrivate())
                .thumbnailUrl(room.getThumbnailUrl())  // 썸네일 URL
                .maxParticipants(room.getMaxParticipants())
                .currentParticipants((int) currentParticipants)  // Redis에서 조회한 실시간 값
                .status(room.getStatus())
                .allowCamera(room.isAllowCamera())
                .allowAudio(room.isAllowAudio())
                .allowScreenShare(room.isAllowScreenShare())
                .createdBy(room.getCreatedBy().getNickname())
                .createdAt(room.getCreatedAt())
                .members(members)
                .build();
    }
}
