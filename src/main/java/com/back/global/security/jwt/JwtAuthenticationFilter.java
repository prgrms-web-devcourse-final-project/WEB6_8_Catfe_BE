package com.back.global.security.jwt;

import com.back.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증을 처리하는 필터
 * - 모든 요청에 대해 JWT 토큰을 검사
 * - 토큰이 유효하면 Authentication 객체를 생성하여 SecurityContext에 저장
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Request Header에서 토큰 추출
            String token = resolveToken(request);

            // 토큰이 유효한 경우에만 Authentication 객체 생성 및 SecurityContext에 저장
            if (token != null) {
                jwtTokenProvider.validateAccessToken(token);
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // 다음 필터로 요청 전달
            filterChain.doFilter(request, response);
        } catch (CustomException ex) {
            // SecurityContext 초기화
            SecurityContextHolder.clearContext();

            // request attribute에 ErrorCode 저장
            request.setAttribute("errorCode", ex.getErrorCode());

            // 인증 실패 처리
            jwtAuthenticationEntryPoint.commence(
                    request, response,
                    new InsufficientAuthenticationException("Custom auth error")
            );
        }

    }

    /**
     * Request의 Authorization 헤더에서 JWT 토큰을 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
