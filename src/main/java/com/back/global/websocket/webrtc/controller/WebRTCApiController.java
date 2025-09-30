package com.back.global.websocket.webrtc.controller;

import com.back.global.common.dto.RsData;
import com.back.global.websocket.webrtc.dto.ice.IceServerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
@Tag(name = "WebRTC API", description = "WebRTC 시그널링 및 ICE 서버 관련 REST API")
public class WebRTCApiController {

    // ICE 서버 설정 조회
    @GetMapping("/ice-servers")
    @Operation(
            summary = "ICE 서버 설정 조회",
            description = "WebRTC 연결에 필요한 STUN/TURN 서버 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<RsData<IceServerConfig>> getIceServers(
            @Parameter(description = "사용자 ID (선택)") @RequestParam(required = false) Long userId,
            @Parameter(description = "방 ID (선택)") @RequestParam(required = false) Long roomId) {

        log.info("ICE 서버 설정 요청 - userId: {}, roomId: {}", userId, roomId);

        // 기본 Google STUN 서버 사용
        IceServerConfig config = IceServerConfig.withDefaultStunServers();

        log.info("ICE 서버 설정 제공 완료 - STUN 서버 {}개", config.iceServers().size());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("ICE 서버 설정 조회 성공", config));
    }

    // WebRTC 서비스 상태 확인
    @GetMapping("/health")
    @Operation(
            summary = "WebRTC 서비스 상태 확인",
            description = "WebRTC 시그널링 서버의 상태를 확인합니다."
    )
    @ApiResponse(responseCode = "200", description = "정상 작동 중")
    public ResponseEntity<RsData<String>> healthCheck() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("WebRTC 서비스 정상 작동 중"));
    }
}