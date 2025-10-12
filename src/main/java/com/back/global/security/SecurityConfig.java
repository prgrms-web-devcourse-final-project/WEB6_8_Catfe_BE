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
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS Preflight 요청 허용
                                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                                .requestMatchers("api/ws/**", "/ws/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                                .requestMatchers("/api/rooms/*/messages/**").permitAll()  //스터디 룸 내에 잡혀있어 있는 채팅 관련 전체 허용
                                //.requestMatchers("/api/rooms/RoomChatApiControllerTest").permitAll() // 테스트용 임시 허용
                                .requestMatchers("/","/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger 허용
                                .requestMatchers("/h2-console/**").permitAll() // H2 Console 허용
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
                                "https://www.catfe.com" // Catfe 프론트 운영 서버
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
