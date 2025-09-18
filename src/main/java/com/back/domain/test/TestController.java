package com.back.domain.test;

import com.back.global.common.dto.RsData;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/")
    public ResponseEntity<RsData<Void>> hello() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(RsData.fail(ErrorCode.BAD_REQUEST));
    }
}
