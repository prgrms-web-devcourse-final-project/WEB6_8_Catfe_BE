package com.back.domain.file.controller;

import com.back.domain.file.dto.PresignedUrlResponseDto;
import com.back.domain.file.service.PresignedUrlService;
import com.back.global.common.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(
        name = "Presigned URL API",
        description = "S3 업로드용 Presigned URL 발급 및 S3 객체 삭제 API"
)
public class PresignedUrlController {
    private final PresignedUrlService presignedUrlService;

    /**
     * 업로드용 Presigned URL 발급 (대용량 업데이트 용)
     */
    @GetMapping("/presigned-upload")
    @Operation(
            summary = "S3 업로드용 Presigned URL 발급",
            description = "S3 Direct Upload를 위해 Presigned URL과 업로드 대상 객체 정보를 반환합니다."
    )
    public ResponseEntity<RsData<PresignedUrlResponseDto>> getPresignedUploadUrl(
            @RequestParam String folder,
            @RequestParam String fileName
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success(
                        "URL 발급 성공", presignedUrlService.generateUploadPresignedUrl(
                                folder,
                                fileName
                        )
                ));
    }

    /**
     * objectURL 을 통해 S3 객체 삭제
     */
    @DeleteMapping
    @Operation(
            summary = "S3 객체 삭제",
            description = "objectUrl에서 key를 추출하여 해당 S3 객체를 삭제합니다."
    )
    public ResponseEntity<RsData<Void>> deleteImage(@RequestParam String objectUrl) {
        presignedUrlService.deleteFile(objectUrl);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(RsData.success("S3 오브젝트 삭제 성공"));
    }
}