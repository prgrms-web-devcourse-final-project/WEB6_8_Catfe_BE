package com.back.global.websocket.util;

import com.back.global.security.user.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WebSocketAuthHelper {

    // WebSocket에서 인증된 사용자 정보 추출
    public static CustomUserDetails extractUserDetails(Principal principal) {
        if (principal instanceof Authentication auth) {
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof CustomUserDetails userDetails) {
                return userDetails;
            }
        }
        return null;
    }
}