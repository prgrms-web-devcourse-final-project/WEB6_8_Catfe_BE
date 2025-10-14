package com.back.global.security;

import com.back.global.security.jwt.JwtAccessDeniedHandler;
import com.back.global.security.jwt.JwtAuthenticationEntryPoint;
import com.back.global.security.jwt.JwtAuthenticationFilter;
import com.back.global.security.oauth.CustomOAuth2UserService;
import com.back.global.security.oauth.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 인가 규칙 설정
                .authorizeHttpRequests(
                        auth -> auth
                                // CORS Preflight
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // 개발 및 모니터링용
                                .requestMatchers(
                                        "/",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                ).permitAll()                                      // Swagger
                                .requestMatchers("/h2-console/**").permitAll()   // H2 콘솔
                                .requestMatchers("/actuator/health").permitAll() // Health check

                                // 인증/인가
                                .requestMatchers(
                                        "/api/auth/**",
                                        "/oauth2/**",
                                        "/login/oauth2/**"
                                ).permitAll()

                                // WebSocket
                                .requestMatchers(
                                        "api/ws/**",
                                        "/ws/**"
                                ).permitAll()

                                // 스터디룸 관련
                                .requestMatchers("/api/rooms/*/messages/**").permitAll()  // 방 내 채팅 메시지
                                .requestMatchers(HttpMethod.GET,
                                        "/api/rooms",             // 전체 목록 조회
                                        "/api/rooms/all",         // 전체 방 목록
                                        "/api/rooms/public",      // 공개 방 목록
                                        "/api/rooms/popular",     // 인기 방 목록
                                        "/api/rooms/*"            // 방 상세 조회
                                ).permitAll()
                                //.requestMatchers("/api/rooms/RoomChatApiControllerTest").permitAll() // 테스트용

                                // 커뮤니티 관련
                                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                                .requestMatchers("/api/rooms/*/messages/**").permitAll()  //스터디 룸 내에 잡혀있어 있는 채팅 관련 전체 허용
                                // 방 목록 조회 API 비로그인 허용
                                .requestMatchers(HttpMethod.GET, "/api/rooms").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/rooms/all").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/rooms/public").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/rooms/popular").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/rooms/*").permitAll() // 방 상세 조회
                                //.requestMatchers("/api/rooms/RoomChatApiControllerTest").permitAll() // 테스트용 임시 허용
                                .requestMatchers("/","/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger 허용
//                                .requestMatchers("/h2-console/**").permitAll() // H2 Console 허용
                                .requestMatchers("/actuator/health").permitAll() // 헬스 체크 허용
                                .requestMatchers("/file/**").permitAll() // 파일 관련 요청 모두 허용(테스트 완료 후, 요청제한 예정)
                                .anyRequest().authenticated()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                // 인증/인가 실패 핸들러
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler)            // 403
                )

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .headers(
                        headers -> headers
                                .frameOptions(
                                        HeadersConfigurer.FrameOptionsConfig::sameOrigin
                                )
                )
                .csrf(
                        AbstractHttpConfigurer::disable
                )
                .cors(
                        Customizer.withDefaults()
                );

        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:3000", // Catfe 프론트 개발 서버
                                "https://www.catfe.site" // Catfe 프론트 운영 서버
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
