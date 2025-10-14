package com.back.global.websocket.config;

import com.back.global.security.user.CustomUserDetails;
import com.back.global.security.jwt.JwtTokenProvider;
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
     * 
     * STOMP 하트비트 설정(임시 주석 상태):
     * - 25초마다 자동 하트비트 전송 (쓰기 비활성 시)
     * - 25초 이상 응답 없으면 연결 종료 (읽기 비활성 시)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
                //.setHeartbeatValue(new long[]{25000, 25000}) // [서버→클라이언트, 클라이언트→서버]
                //.setTaskScheduler(heartBeatScheduler());
        
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**(임시 주석 상태)
     * STOMP 하트비트 전용 스케줄러!!
     * - 별도 스레드 풀로 하트비트 처리
     * - 메인 비즈니스 로직에 영향 없음

    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("wss-heartbeat-");
        scheduler.initialize();
        log.info("STOMP 하트비트 스케줄러 초기화 완료 - 주기: 25초");
        return scheduler;
    }
     */

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
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {

                    log.info("🔥 [INTERCEPT] Command: {}, Dest: {}, SessionId: {}",
                            accessor.getCommand(),
                            accessor.getDestination(),
                            accessor.getSessionId());

                    try {
                        // CONNECT 시점에서 JWT 토큰 인증
                        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                            authenticateUser(accessor);
                        }

                        // SEND 시점에서 인증 확인 및 활동 시간 업데이트
                        else if (StompCommand.SEND.equals(accessor.getCommand())) {
                            log.info("🔥 [SEND] Dest: {}, User: {}",
                                    accessor.getDestination(),
                                    accessor.getUser() != null ? accessor.getUser().getName() : "null");

                            validateAuthenticationAndUpdateActivity(accessor);
                        }
                    } catch (Exception e) {

                        log.error("🔥 [INTERCEPT ERROR] Command: {}, Dest: {}, Error: {}",
                                accessor.getCommand(),
                                accessor.getDestination(),
                                e.getMessage(), e);

                        // 예외를 다시 던져서 메시지 차단
                        throw e;
                    }
                }

                log.info("🔥 [INTERCEPT] Message passing through");
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
            jwtTokenProvider.validateAccessToken(token);

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
     * 메시지 전송 시 인증 상태 확인
     */
    private void validateAuthenticationAndUpdateActivity(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new RuntimeException("인증이 필요합니다");
        }

        Authentication auth = (Authentication) accessor.getUser();
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            Long userId = userDetails.getUserId();

            // 전역 세션 활동 시간 업데이트
            try {
                sessionManager.updateLastActivity(userId);
            } catch (Exception e) {
                log.warn("활동 시간 업데이트 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
            }

            log.debug("인증된 사용자 메시지 전송 - 사용자: {} (ID: {}), 목적지: {}",
                    userDetails.getUsername(), userId, accessor.getDestination());
        }
    }
}
