package com.back.global.security;

import com.back.global.common.dto.RsData;
import com.back.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패(401 Unauthorized) 처리 클래스
 * - JwtAuthenticationFilter에서 토큰이 없거나 잘못된 경우
 * - 인증되지 않은 사용자가 보호된 API에 접근하려는 경우
 * - Json 형태로 에러 응답을 반환
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        RsData<Void> body = RsData.fail(ErrorCode.UNAUTHORIZED);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
