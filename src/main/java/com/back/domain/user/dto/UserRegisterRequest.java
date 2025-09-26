package com.back.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 회원 가입 요청을 나타내는 DTO
 *
 * @param username 사용자의 로그인 id
 * @param email    사용자의 이메일 주소
 * @param password 사용자의 비밀번호
 * @param nickname 사용자의 별명
 */
public record UserRegisterRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank @Size(max = 20) String nickname
) {}
