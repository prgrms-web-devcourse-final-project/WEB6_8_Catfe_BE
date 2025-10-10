package com.back.domain.file.dto;

import com.back.domain.file.entity.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUpdateRequestDto {
    @NotNull(message = "파일 입력은 필수입니다.")
    private MultipartFile multipartFile;

    @NotBlank(message = "entityType은 필수입니다.")
    private EntityType entityType;

    @NotBlank(message = "entityId는 필수입니다.")
    private Long entityId;
}
