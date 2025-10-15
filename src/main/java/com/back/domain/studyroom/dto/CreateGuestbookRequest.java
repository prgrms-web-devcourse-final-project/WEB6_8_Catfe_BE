package com.back.domain.studyroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGuestbookRequest {
    
    @NotBlank(message = "방명록 내용은 필수입니다")
    @Size(max = 500, message = "방명록은 500자를 초과할 수 없습니다")
    private String content;
}
