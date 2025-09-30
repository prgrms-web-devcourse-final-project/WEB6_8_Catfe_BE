package com.back.domain.study.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TodoRequestDto(
        @NotBlank(message = "할 일 설명은 필수입니다.")
        String description,
        @NotNull(message = "날짜는 필수입니다.")
        LocalDate date
) {
}
