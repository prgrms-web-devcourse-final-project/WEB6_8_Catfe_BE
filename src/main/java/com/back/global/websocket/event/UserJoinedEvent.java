package com.back.global.websocket.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserJoinedEvent {
    private final String type = "USER_JOINED";
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Long avatarId;
}