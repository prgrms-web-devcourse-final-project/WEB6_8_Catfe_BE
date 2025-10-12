package com.back.domain.file.controller;

import com.back.domain.file.dto.*;
import com.back.domain.file.entity.EntityType;
import com.back.domain.file.service.FileService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
@Validated
public class FileController {
    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RsData<FileUploadResponseDto>> uploadFile(
            @ModelAttribute @Valid FileUploadRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        FileUploadResponseDto res = fileService.uploadFile(
                req.getMultipartFile(),
                user.getUserId()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("파일 업로드 성공", res));
    }

    @GetMapping(value = "/read/{attachmentId}")
    public ResponseEntity<RsData<FileReadResponseDto>> getFile(
            @PathVariable("attachmentId") Long attachmentId
    ) {
        FileReadResponseDto res = fileService.getFile(attachmentId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("파일 조회 성공", res));
    }

    @PutMapping(value = "/update/{attachmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RsData<FileUpdateResponseDto>> updateFile(
            @PathVariable("attachmentId") Long attachmentId,
            @ModelAttribute @Valid FileUpdateRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        FileUpdateResponseDto res = fileService.updateFile(
                attachmentId,
                req.getMultipartFile(),
                user.getUserId()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("파일 업데이트 성공", res));
    }

    @DeleteMapping(value = "/delete/{attachmentId}")
    public ResponseEntity<RsData<Void>> deleteFile(
            @PathVariable("attachmentId") Long attachmentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        fileService.deleteFile(
                attachmentId,
                user.getUserId()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("파일 삭제 성공"));
    }
}
