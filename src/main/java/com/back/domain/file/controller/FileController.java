package com.back.domain.file.controller;

import com.back.domain.file.dto.*;
import com.back.domain.file.service.FileService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(
        name =  "파일 관리 API",
        description = "MultipartFile 기반 파일 업로드, 조회, 업데이트, 삭제 기능을 제공"
)
public class FileController {
    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "파일 업로드",
            description = "MultipartFile을 업로드하고, 저장된 파일의 식별자(attachmentId)와 Public URL을 반환합니다."
    )
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
    @Operation(
            summary = "파일 정보 조회",
            description = "attachmentId를 이용해 저장된 파일 정보를 조회하고, 해당 파일의 Public URL을 반환합니다."
    )
    public ResponseEntity<RsData<FileReadResponseDto>> getFile(
            @PathVariable("attachmentId") Long attachmentId
    ) {
        FileReadResponseDto res = fileService.getFile(attachmentId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("파일 조회 성공", res));
    }


    @PutMapping(value = "/update/{attachmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "파일 업데이트",
            description = "기존 attachmentId에 연결된 파일을 새로운 MultipartFile로 교체하고, 변경된 파일의 Public URL을 반환합니다."
    )
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
    @Operation(
            summary = "파일 삭제",
            description = "attachmentId에 해당하는 파일을 삭제합니다."
    )
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
