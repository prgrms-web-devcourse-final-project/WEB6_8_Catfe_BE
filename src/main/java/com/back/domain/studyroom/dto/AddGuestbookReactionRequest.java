package com.back.domain.studyroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddGuestbookReactionRequest {
    
    @NotBlank(message = "이모지는 필수입니다")
    @Size(max = 10, message = "이모지는 10자를 초과할 수 없습니다")
    private String emoji;
}
