package com.back.global.websocket.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserLeftEvent {
    private final String type = "USER_LEFT";
    private Long userId;
}