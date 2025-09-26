package com.back.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 사용자 프로필 수정 요청 DTO
 *
 * @param nickname          사용자의 별명
 * @param profileImageUrl   사용자의 프로필 이미지 URL
 * @param bio               사용자의 자기소개
 * @param birthDate         사용자의 생년월일
 */
public record UpdateUserProfileRequest(
        @NotBlank @Size(max = 20) String nickname,
        String profileImageUrl,
        @Size(max = 255) String bio,
        LocalDate birthDate
) {}
