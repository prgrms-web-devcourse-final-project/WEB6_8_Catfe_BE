package com.back.global.security.jwt;

import com.back.global.common.dto.RsData;
import com.back.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인가 실패(403 Forbidden) 처리 클래스
 * - 인증은 되었으나, 권한(Role)이 부족한 경우
 * - Json 형태로 에러 응답을 반환
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        RsData<Void> body = RsData.fail(ErrorCode.ACCESS_DENIED);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
