package com.back.global.config;

import com.back.global.security.CustomUserDetails;
import com.back.global.security.JwtTokenProvider;
import com.back.global.websocket.service.WebSocketSessionManager;
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
    private final WebSocketSessionManager sessionManager;

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
     * JWT 인증 인터셉터 및 세션 관리 로직 등록
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT 인증 + 세션 관리 인터셉터 등록
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

                    // SEND 시점에서 인증 확인 및 활동 시간 업데이트
                    else if (StompCommand.SEND.equals(accessor.getCommand())) {
                        validateAuthenticationAndUpdateActivity(accessor);
                    }

                    // SUBSCRIBE 시점에서 방 입장 처리
                    else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        handleRoomSubscription(accessor);
                    }

                    // UNSUBSCRIBE 시점에서 방 퇴장 처리
                    else if (StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
                        handleRoomUnsubscription(accessor);
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

            log.info("WebSocket 인증 성공 - 사용자: {} (ID: {}), 세션: {}",
                    userDetails.getUsername(), userDetails.getUserId(), accessor.getSessionId());

        } catch (Exception e) {
            log.error("WebSocket 인증 실패: {}", e.getMessage());
            throw new RuntimeException("WebSocket 인증에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 메시지 전송 시 인증 상태 확인 및 활동 시간 업데이트
     */
    private void validateAuthenticationAndUpdateActivity(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new RuntimeException("인증이 필요합니다");
        }

        // 인증된 사용자 정보 추출 및 활동 시간 업데이트
        Authentication auth = (Authentication) accessor.getUser();
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            Long userId = userDetails.getUserId();

            // 사용자 활동 시간 업데이트
            sessionManager.updateLastActivity(userId);

            log.debug("인증된 사용자 메시지 전송 - 사용자: {} (ID: {}), 목적지: {}",
                    userDetails.getUsername(), userId, accessor.getDestination());
        }
    }

    /**
     * 방 채팅 구독 시 방 입장 처리
     */
    private void handleRoomSubscription(StompHeaderAccessor accessor) {
        try {
            String destination = accessor.getDestination();

            // 방 채팅 구독인지 확인: /topic/rooms/{roomId}/chat
            if (destination != null && destination.matches("/topic/rooms/\\d+/chat")) {
                Long roomId = extractRoomIdFromDestination(destination);

                if (roomId != null && accessor.getUser() != null) {
                    Authentication auth = (Authentication) accessor.getUser();
                    if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                        Long userId = userDetails.getUserId();

                        // 방 입장 처리
                        sessionManager.joinRoom(userId, roomId);

                        log.info("방 입장 처리 완료 - 사용자: {} (ID: {}), 방: {}",
                                userDetails.getUsername(), userId, roomId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("방 구독 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 방 채팅 구독 해제 시 방 퇴장 처리
     */
    private void handleRoomUnsubscription(StompHeaderAccessor accessor) {
        try {
            String destination = accessor.getDestination();

            // 방 채팅 구독 해제인지 확인: /topic/rooms/{roomId}/chat
            if (destination != null && destination.matches("/topic/rooms/\\d+/chat")) {
                Long roomId = extractRoomIdFromDestination(destination);

                if (roomId != null && accessor.getUser() != null) {
                    Authentication auth = (Authentication) accessor.getUser();
                    if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                        Long userId = userDetails.getUserId();

                        // 방 퇴장 처리
                        sessionManager.leaveRoom(userId, roomId);

                        log.info("방 퇴장 처리 완료 - 사용자: {} (ID: {}), 방: {}",
                                userDetails.getUsername(), userId, roomId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("방 구독 해제 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * destination에서 방 ID 추출
     * ex) "/topic/rooms/123/chat" -> 123
     */
    private Long extractRoomIdFromDestination(String destination) {
        try {
            if (destination == null) return null;

            String[] parts = destination.split("/");
            if (parts.length >= 4 && "rooms".equals(parts[2])) {
                return Long.parseLong(parts[3]);
            }
        } catch (NumberFormatException e) {
            log.warn("방 ID 추출 실패 - destination: {}, 에러: {}", destination, e.getMessage());
        }
        return null;
    }
}