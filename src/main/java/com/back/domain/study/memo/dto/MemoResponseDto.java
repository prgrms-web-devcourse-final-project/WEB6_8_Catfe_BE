package com.back.domain.study.memo.dto;

import com.back.domain.study.memo.entity.Memo;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class MemoResponseDto {
    private Long id;
    private LocalDate date;
    private String description;

    // 엔티티 -> DTO 변환
    public static MemoResponseDto from(Memo memo) {
        MemoResponseDto dto = new MemoResponseDto();
        dto.id = memo.getId();
        dto.date = memo.getDate();
        dto.description = memo.getDescription();
        return dto;
    }
}
