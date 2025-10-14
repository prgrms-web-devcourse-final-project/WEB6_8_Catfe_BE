package com.back.domain.studyroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방 비밀번호 설정 요청 (비밀번호가 없는 방에 비밀번호 추가)")
public class SetRoomPasswordRequest {
    
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 4, max = 20, message = "비밀번호는 4~20자 사이여야 합니다.")
    @Schema(description = "설정할 비밀번호", example = "1234", required = true)
    private String newPassword;
}
