package com.back.domain.study.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class MemoRequestDto {
    private LocalDate date;

    private String description;

}
