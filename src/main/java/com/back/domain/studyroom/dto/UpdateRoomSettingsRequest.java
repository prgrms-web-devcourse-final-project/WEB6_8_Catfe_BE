package com.back.domain.studyroom.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomSettingsRequest {
    @NotBlank(message = "방 제목은 필수입니다")
    @Size(max = 100, message = "방 제목은 100자를 초과할 수 없습니다")
    private String title;
    
    @Size(max = 500, message = "방 설명은 500자를 초과할 수 없습니다")
    private String description;
    
    @Min(value = 2, message = "최소 2명 이상이어야 합니다")
    @Max(value = 100, message = "최대 100명까지 가능합니다")
    private Integer maxParticipants;
    
    // 방 썸네일 이미지 URL (선택)
    @Size(max = 500, message = "썸네일 URL은 500자를 초과할 수 없습니다")
    private String thumbnailUrl;
    
    // ===== WebRTC 설정 (추후 팀원 구현 시 주석 해제) =====
    // WebRTC 기능은 방 생성 이후 별도 API로 관리 예정
    // 현재는 방 생성 시의 useWebRTC 값으로만 초기 설정됨
    
    // private Boolean allowCamera = true;
    // private Boolean allowAudio = true;
    // private Boolean allowScreenShare = true;
}
