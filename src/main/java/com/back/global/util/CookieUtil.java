package com.back.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    /**
     * 쿠키 추가 메서드
     *
     * @param response HttpServletResponse 객체
     * @param name     쿠키 이름
     * @param value    쿠키 값
     * @param maxAge   쿠키 수명 (초 단위, 음수: 브라우저 종료 시 삭제, 0: 즉시 삭제)
     * @param path     쿠키 경로 (null이면 "/")
     * @param secure   HTTPS에서만 전송 여부
     */
    public static void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAge,
            String path,
            boolean secure
    ) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);   // JS 접근 차단
        cookie.setSecure(secure);   // HTTPS에서만 전송 (dev/prod 분기 권장)
        cookie.setPath(path != null ? path : "/"); // 기본 path = /
        cookie.setMaxAge(maxAge);

        // SameSite 설정 → Servlet Cookie API엔 없어서 수동 헤더 추가 필요
        String sameSite = secure ? "None" : "Lax";
        // cross-site 환경이면 None + Secure, same-site면 Strict/Lax 선택
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Max-Age=%d; Path=%s; HttpOnly; Secure=%s; SameSite=%s",
                        cookie.getName(),
                        cookie.getValue(),
                        cookie.getMaxAge(),
                        cookie.getPath(),
                        cookie.getSecure() ? "true" : "false",
                        sameSite
                )
        );
    }

    /**
     * 쿠키 삭제 메서드
     *
     * @param response HttpServletResponse 객체
     * @param name     삭제할 쿠키 이름
     * @param path     쿠키 경로 (null이면 "/")
     * @param secure   HTTPS에서만 전송 여부
     */
    public static void clearCookie(HttpServletResponse response, String name, String path, boolean secure) {
        addCookie(response, name, "", 0, path, secure);
    }
}
