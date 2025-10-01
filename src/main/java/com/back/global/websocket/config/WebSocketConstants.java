package com.back.global.websocket.config;

import java.time.Duration;

public final class WebSocketConstants {

    private WebSocketConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다.");
    }

    // ===== TTL & Timeout 설정 =====

    /**
     * WebSocket 세션 TTL (6분)
     * - Heartbeat로 연장됨
     */
    public static final Duration SESSION_TTL = Duration.ofMinutes(6);

    /**
     * Heartbeat 권장 간격 (5분)
     * - 클라이언트가 이 주기로 Heartbeat 전송 권장
     */
    public static final Duration HEARTBEAT_INTERVAL = Duration.ofMinutes(5);

    // ===== Redis Key 패턴 =====

    /**
     * 사용자 세션 정보 저장 Key
     * - 패턴: ws:user:{userId}
     * - 값: WebSocketSessionInfo
     */
    public static final String USER_SESSION_KEY_PREFIX = "ws:user:";

    /**
     * 세션 → 사용자 매핑 Key
     * - 패턴: ws:session:{sessionId}
     * - 값: userId (Long)
     */
    public static final String SESSION_USER_KEY_PREFIX = "ws:session:";

    /**
     * 방별 참가자 목록 Key
     * - 패턴: ws:room:{roomId}:users
     * - 값: Set<userId>
     */
    public static final String ROOM_USERS_KEY_PREFIX = "ws:room:";
    public static final String ROOM_USERS_KEY_SUFFIX = ":users";

    // ===== Key 빌더 헬퍼 메서드 =====

    public static String buildUserSessionKey(Long userId) {
        return USER_SESSION_KEY_PREFIX + userId;
    }

    public static String buildSessionUserKey(String sessionId) {
        return SESSION_USER_KEY_PREFIX + sessionId;
    }

    public static String buildRoomUsersKey(Long roomId) {
        return ROOM_USERS_KEY_PREFIX + roomId + ROOM_USERS_KEY_SUFFIX;
    }

    public static String buildUserSessionKeyPattern() {
        return USER_SESSION_KEY_PREFIX + "*";
    }

    // ===== API 응답용 =====

    public static String getSessionTTLDescription() {
        return SESSION_TTL.toMinutes() + "분 (Heartbeat 방식)";
    }

    public static String getHeartbeatIntervalDescription() {
        return HEARTBEAT_INTERVAL.toMinutes() + "분";
    }
}