package com.back.domain.file.dto;

import com.back.domain.file.entity.EntityType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUpdateRequestDto {
    private MultipartFile multipartFile;

    @NotBlank(message = "entityType은 필수입니다.")
    private EntityType entityType;

    @NotBlank(message = "entityId는 필수입니다.")
    private Long entityId;
}
