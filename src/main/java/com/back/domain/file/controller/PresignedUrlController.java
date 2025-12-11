package com.back.domain.file.controller;

import com.back.domain.file.dto.PresignedUrlResponseDto;
import com.back.global.common.dto.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
public class PresignedController {

    public ResponseEntity<RsData<PresignedUrlResponseDto>> getPresignedUploadUrl(
          @RequestParam String folder,
          @RequestParam String fileName
    ) {
        String presignedUrl =
    }
}
