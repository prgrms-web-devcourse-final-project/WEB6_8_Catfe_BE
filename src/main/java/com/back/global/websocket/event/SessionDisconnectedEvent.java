package com.back.global.websocket.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * WebSocket 세션 연결이 종료되었을 때 발생하는 내부 이벤트.
 * 이 이벤트는 SessionManager가 발행하고, ParticipantService가 구독하여 처리합니다.
 */
@Getter
public class SessionDisconnectedEvent extends ApplicationEvent {

    private final Long userId;

    public SessionDisconnectedEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }
}

