package com.back.global.security.oauth;

import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserToken;
import com.back.domain.user.common.repository.UserRepository;
import com.back.domain.user.common.repository.UserTokenRepository;
import com.back.global.common.dto.RsData;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.domain.user.common.util.CookieUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * OAuth2 로그인 성공 시 처리 핸들러
 *
 * 주요 기능:
 * 1. 인증된 사용자 정보로 DB 조회
 * 2. JWT Access Token 및 Refresh Token 생성
 * 3. Refresh Token을 HttpOnly 쿠키에 저장
 * 4. Access Token 및 사용자 정보를 JSON 형태로 응답
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final ObjectMapper objectMapper;

    @Value("${frontend.base-url}")
    private String FRONTEND_BASE_URL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            // 인증 정보에서 사용자 정보 추출 및 DB 조회
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // 토큰 생성
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

            // DB에 Refresh Token 저장
            UserToken userToken = new UserToken(
                    user,
                    refreshToken,
                    LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationInSeconds())
            );
            userTokenRepository.save(userToken);

            // Refresh Token을 HttpOnly 쿠키에 저장
            CookieUtil.addCookie(
                    response,
                    "refreshToken",
                    refreshToken,
                    (int) jwtTokenProvider.getRefreshTokenExpirationInSeconds(),
                    "/",
                    true
            );

            // 프론트엔드 리다이렉트
            response.sendRedirect(FRONTEND_BASE_URL + "/login/oauth2");
        } catch (CustomException e) {
            handleException(response, e);
        } catch (Exception e) {
            handleException(response, new CustomException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED));
        }
    }

    private void handleException(HttpServletResponse response, CustomException e) throws IOException {
        RsData<Object> rsData = RsData.fail(e.getErrorCode());
        response.setStatus(e.getErrorCode().getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), rsData);
    }
}
