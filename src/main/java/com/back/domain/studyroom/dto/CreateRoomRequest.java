package com.back.domain.studyroom.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    @NotBlank(message = "방 제목은 필수입니다")
    @Size(max = 100, message = "방 제목은 100자를 초과할 수 없습니다")
    private String title;
    
    @Size(max = 500, message = "방 설명은 500자를 초과할 수 없습니다")
    private String description;
    
    private Boolean isPrivate = false;
    
    private String password;
    
    @Min(value = 2, message = "최소 2명 이상이어야 합니다")
    @Max(value = 100, message = "최대 100명까지 가능합니다")
    private Integer maxParticipants = 10;
    
    // WebRTC 통합 제어 필드 (카메라, 오디오, 화면공유를 한 번에 제어)
    // true: WebRTC 기능 전체 활성화
    // false: WebRTC 기능 전체 비활성화
    // null: 디폴트 true로 처리
    private Boolean useWebRTC = true;
}
