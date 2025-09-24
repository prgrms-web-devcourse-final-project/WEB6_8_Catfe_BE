package com.back.global.config;

import com.back.global.security.CustomUserDetails;
import com.back.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 메시지 브로커 설정
     * - /topic: 1:N 브로드캐스트 (방 채팅)
     * - /queue: 1:1 메시지 (개인 DM)
     * - /app: 클라이언트에서 서버로 메시지 전송 시 prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트 등록
     * 클라이언트가 WebSocket 연결을 위해 사용할 엔드포인트
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 모든 도메인 허용 (개발용)
                .withSockJS(); // SockJS 사용
    }

    /**
     * WebSocket 메시지 채널 설정
     * JWT 인증 인터셉터 등록
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT 인증 인터셉터 등록
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    log.debug("WebSocket 메시지 처리 - Command: {}, Destination: {}, SessionId: {}",
                            accessor.getCommand(), accessor.getDestination(), accessor.getSessionId());

                    // CONNECT 시점에서 JWT 토큰 인증
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        authenticateUser(accessor);
                    }

                    // SEND 시점에서도 인증 확인 (추가 보안)
                    else if (StompCommand.SEND.equals(accessor.getCommand())) {
                        validateAuthentication(accessor);
                    }
                }

                return message;
            }
        });
    }

    /**
     * WebSocket 연결 시 JWT 토큰 인증
     */
    private void authenticateUser(StompHeaderAccessor accessor) {
        try {
            // Authorization 헤더에서 JWT 토큰 추출
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("인증 토큰이 필요합니다");
            }

            String token = authHeader.substring(7); // "Bearer " 제거

            // JWT 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                throw new RuntimeException("유효하지 않은 인증 토큰입니다");
            }

            // 토큰에서 사용자 정보 추출
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // 세션에 사용자 정보 저장
            accessor.setUser(authentication);

        } catch (Exception e) {
            throw new RuntimeException("WebSocket 인증에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 메시지 전송 시 인증 상태 확인
     */
    private void validateAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new RuntimeException("인증이 필요합니다");
        }

        // 인증된 사용자 정보 로깅
        Authentication auth = (Authentication) accessor.getUser();
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            log.debug("인증된 사용자 메시지 전송 - 사용자: {} (ID: {}), 목적지: {}",
                    userDetails.getUsername(), userDetails.getUserId(), accessor.getDestination());
        }
    }
}
