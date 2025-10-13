package com.back.domain.file.dto;

import com.back.domain.file.entity.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadRequestDto {
    @NotNull(message = "파일 입력은 필수입니다.")
    private MultipartFile multipartFile;
}
