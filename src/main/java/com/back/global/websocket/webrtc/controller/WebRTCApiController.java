package com.back.global.websocket.webrtc.controller;

import com.back.global.common.dto.RsData;
import com.back.global.websocket.webrtc.config.WebRTCProperties;
import com.back.global.websocket.webrtc.dto.ice.IceServer;
import com.back.global.websocket.webrtc.dto.ice.IceServerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webrtc")
@EnableConfigurationProperties(WebRTCProperties.class)
@Tag(name = "WebRTC API", description = "WebRTC 시그널링 및 ICE 서버 관련 REST API")
public class WebRTCApiController {

    private final WebRTCProperties webRTCProperties;

    @GetMapping("/ice-servers")
    @Operation(summary = "ICE 서버 설정 조회")
    public ResponseEntity<RsData<IceServerConfig>> getIceServers(
            @Parameter(description = "사용자 ID (선택)") @RequestParam(required = false) Long userId,
            @Parameter(description = "방 ID (선택)") @RequestParam(required = false) Long roomId) {

        log.info("ICE 서버 설정 요청 - userId: {}, roomId: {}", userId, roomId);

        List<IceServer> iceServers =
                webRTCProperties.iceServers().stream()
                        .map(s -> new IceServer(s.urls(), s.username(), s.credential()))
                        .toList();

        IceServerConfig config = new IceServerConfig(iceServers);

        log.info("ICE 서버 설정 제공 완료 - STUN/TURN 서버 {}개", config.iceServers().size());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("ICE 서버 설정 조회 성공", config));
    }

    @GetMapping("/health")
    @Operation(summary = "WebRTC 서비스 상태 확인")
    public ResponseEntity<RsData<String>> healthCheck() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("WebRTC 서비스 정상 작동 중"));
    }
}