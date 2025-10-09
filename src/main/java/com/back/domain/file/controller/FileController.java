package com.back.domain.file.controller;

import com.back.domain.file.dto.FileUploadRequestDto;
import com.back.domain.file.dto.FileUploadResponseDto;
import com.back.domain.file.service.FileService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
public class FileController {
    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RsData<FileUploadResponseDto>> uploadFile(
            @ModelAttribute FileUploadRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        FileUploadResponseDto res = fileService.uploadFile(
                req.getMultipartFile(),
                req.getEntityType(),
                req.getEntityID(),
                user.getUserId()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("파일 업로드 성공", res));
    }
}
