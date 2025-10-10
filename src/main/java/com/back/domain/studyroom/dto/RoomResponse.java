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
    private Boolean isPrivate;  // 비공개 방 여부 (UI에서 🔒 아이콘 표시용)
    private String thumbnailUrl;  // 썸네일 이미지 URL
    private int currentParticipants;
    private int maxParticipants;
    private RoomStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    
    // WebRTC 설정 정보 (프론트엔드에서 UI 제어용)
    private Boolean allowCamera;
    private Boolean allowAudio;
    private Boolean allowScreenShare;
    
    public static RoomResponse from(Room room, long currentParticipants) {
        return RoomResponse.builder()
                .roomId(room.getId())
                .title(room.getTitle())
                .description(room.getDescription() != null ? room.getDescription() : "")
                .isPrivate(room.isPrivate())  // 비공개 방 여부
                .thumbnailUrl(room.getThumbnailUrl())  // 썸네일 URL
                .currentParticipants((int) currentParticipants)  // Redis에서 조회한 실시간 값
                .maxParticipants(room.getMaxParticipants())
                .status(room.getStatus())
                .createdBy(room.getCreatedBy().getNickname())
                .createdAt(room.getCreatedAt())
                .allowCamera(room.isAllowCamera())
                .allowAudio(room.isAllowAudio())
                .allowScreenShare(room.isAllowScreenShare())
                .build();
    }
    
    /**
     * 비공개 방 정보 마스킹 버전 (전체 목록에서 볼 때 사용)
     * "모든 방" 조회 시 사용 - 비공개 방의 민감한 정보를 숨김
     */
    public static RoomResponse fromMasked(Room room) {
        return RoomResponse.builder()
                .roomId(room.getId())
                .title("🔒 비공개 방")  // 제목 마스킹
                .description("비공개 방입니다")  // 설명 마스킹
                .isPrivate(true)
                .thumbnailUrl(null)      // 썸네일 숨김
                .currentParticipants(0)  // 참가자 수 숨김
                .maxParticipants(0)      // 정원 숨김
                .status(room.getStatus())
                .createdBy("익명")        // 방장 정보 숨김
                .createdAt(room.getCreatedAt())
                .allowCamera(false)      // RTC 정보 숨김
                .allowAudio(false)
                .allowScreenShare(false)
                .build();
    }
}
