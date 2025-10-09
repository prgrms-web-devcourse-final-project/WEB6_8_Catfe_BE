package com.back.domain.file.dto;

import com.back.domain.file.entity.EntityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadRequestDto {
    private MultipartFile multipartFile;
    private EntityType entityType;
    private Long entityID;
}
