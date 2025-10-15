package com.back.domain.studyroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공지사항 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnnouncementRequest {
    
    @NotBlank(message = "공지사항 제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    private String title;
    
    @NotBlank(message = "공지사항 내용은 필수입니다")
    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String content;
}
