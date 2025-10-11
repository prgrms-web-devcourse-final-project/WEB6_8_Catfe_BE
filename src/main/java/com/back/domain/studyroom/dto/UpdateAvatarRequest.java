package com.back.domain.studyroom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아바타 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvatarRequest {
    
    @NotNull(message = "아바타 ID는 필수입니다")
    @Positive(message = "올바른 아바타 ID를 입력해주세요")
    private Long avatarId;
}
