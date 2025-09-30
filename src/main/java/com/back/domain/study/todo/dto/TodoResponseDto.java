package com.back.domain.study.todo.dto;

import com.back.domain.study.todo.entity.Todo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record TodoResponseDto(
        Long id,
        String description,
        boolean isComplete,
        LocalDate date
) {
    // entity -> DTO
    public static TodoResponseDto from(Todo todo) {
        return new TodoResponseDto(
                todo.getId(),
                todo.getDescription(),
                todo.isComplete(),
                todo.getDate()
        );
    }
}
