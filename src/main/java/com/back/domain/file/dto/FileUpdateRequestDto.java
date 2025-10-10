package com.back.domain.file.dto;

import com.back.domain.file.entity.EntityType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUpdateRequestDto {
    private MultipartFile multipartFile;
    private EntityType entityType;
    private Long entityId;
}
