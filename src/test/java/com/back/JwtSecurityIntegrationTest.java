package com.back;

import com.back.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

//    @Test
//    @DisplayName("public 엔드포인트 접근 시 200 OK 반환")
//    void givenNoToken_whenAccessPublic_thenReturn200() throws Exception {
//        mockMvc.perform(get("/api/test/public"))
//                .andExpect(status().isOk());
//    }

    @Test
    @DisplayName("일반 유저가 /me 접근 시 200 OK 반환")
    void givenUserToken_whenAccessMe_thenReturn200() throws Exception {
        // ROLE_USER 토큰 발급
        String userToken = jwtTokenProvider.createAccessToken(3L, "user2", "USER");

        mockMvc.perform(get("/api/test/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("토큰 없이 /me 접근 시 401 Unauthorized 반환")
    void givenNoToken_whenAccessMe_thenReturn401() throws Exception {
        mockMvc.perform(get("/api/test/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("관리자가 /admin 접근 시 200 OK 반환")
    void givenAdminToken_whenAccessAdmin_thenReturn200() throws Exception {
        // ROLE_ADMIN 토큰 발급
        String adminToken = jwtTokenProvider.createAccessToken(2L, "admin1", "ADMIN");

        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 유저가 /admin 접근 시 403 Forbidden 반환")
    void givenUserToken_whenAccessAdmin_thenReturn403() throws Exception {
        // ROLE_USER 토큰 발급
        String userToken = jwtTokenProvider.createAccessToken(1L, "user1", "USER");

        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
