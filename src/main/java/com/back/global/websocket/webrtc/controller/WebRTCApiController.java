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
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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

    @Value("${webrtc.turn.shared-secret}")
    private String turnSharedSecret;

    @Value("${webrtc.turn.server-ip}")
    private String turnServerIp;

    @Value("${webrtc.turn.ttl-seconds}")
    private long turnTtlSeconds;

    @GetMapping("/ice-servers")
    @Operation(summary = "ICE 서버 설정 조회")
    public ResponseEntity<RsData<IceServerConfig>> getIceServers(
            @Parameter(description = "사용자 ID (선택)") @RequestParam(required = false) Long userId,
            @Parameter(description = "방 ID (선택)") @RequestParam(required = false) Long roomId) {

        log.info("ICE 서버 설정 요청 - userId: {}, roomId: {}", userId, roomId);

        List<IceServer> iceServers = new ArrayList<>(
                webRTCProperties.iceServers().stream()
                        .map(s -> new IceServer(s.urls(), s.username(), s.credential()))
                        .toList()
        );

        // 동적으로 시간제한이 있는 TURN 서버 인증 정보 생성
        try {
            // 유효기간 타임스탬프를 생성 (현재 시간 + TTL)
            long expiry = (System.currentTimeMillis() / 1000) + turnTtlSeconds;
            String username = String.valueOf(expiry);

            // HMAC-SHA1 알고리즘과 공유 비밀키를 사용하여 비밀번호(credential) 생성
            Mac sha1Hmac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKey = new SecretKeySpec(turnSharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            sha1Hmac.init(secretKey);
            byte[] hmacBytes = sha1Hmac.doFinal(username.getBytes(StandardCharsets.UTF_8));
            String credential = Base64.getEncoder().encodeToString(hmacBytes);

            // 생성된 TURN 서버 정보를 리스트에 추가
            String turnUrl = "turn:" + turnServerIp + ":3478";
            iceServers.add(IceServer.turn(turnUrl, username, credential));

        } catch (Exception e) {
            log.error("TURN 서버 동적 인증 정보 생성에 실패했습니다. STUN 서버만으로 응답합니다.", e);
            // 인증 정보 생성에 실패하더라도, STUN 서버만으로라도 서비스가 동작하도록 예외를 던지지 않음
        }

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