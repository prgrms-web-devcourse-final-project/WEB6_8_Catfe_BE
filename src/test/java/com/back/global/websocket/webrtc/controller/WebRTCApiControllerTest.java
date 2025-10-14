package com.back.global.websocket.webrtc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@TestPropertySource(properties = {
        "webrtc.turn.shared-secret=test-secret-key-for-junit-12345",
        "webrtc.turn.server-ip=127.0.0.1"
        // ttl-seconds는 application.yml의 기본값 사용
})
@DisplayName("WebRTC API 컨트롤러")
class WebRTCApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("ICE 서버 조회")
    class GetIceServersTest {

        @Test
        @DisplayName("STUN 서버와 동적으로 생성된 TURN 서버 정보를 모두 포함하여 반환")
        void t1() throws Exception {
            // when & then
            MvcResult result = mockMvc.perform(get("/api/webrtc/ice-servers")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                    .andExpect(jsonPath("$.message").value("ICE 서버 설정 조회 성공"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.iceServers").isArray())
                    .andExpect(jsonPath("$.data.iceServers").isNotEmpty())
                    .andExpect(jsonPath("$.data.iceServers[?(@.urls contains 'turn:')].urls").exists())
                    .andExpect(jsonPath("$.data.iceServers[?(@.urls contains 'turn:')].username").exists())
                    .andExpect(jsonPath("$.data.iceServers[?(@.urls contains 'turn:')].credential").exists())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            assertThat(content).contains("stun:");
            assertThat(content).contains("turn:");
        }


        @Test
        @DisplayName("userId, roomId 파라미터")
        void t2() throws Exception {
            // given
            Long userId = 1L;
            Long roomId = 100L;

            // when & then
            mockMvc.perform(get("/api/webrtc/ice-servers")
                            .param("userId", userId.toString())
                            .param("roomId", roomId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                    .andExpect(jsonPath("$.message").value("ICE 서버 설정 조회 성공"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.iceServers").isArray())
                    .andExpect(jsonPath("$.data.iceServers").isNotEmpty());
        }

        @Test
        @DisplayName("userId만")
        void t3() throws Exception {
            // given
            Long userId = 1L;

            // when & then
            mockMvc.perform(get("/api/webrtc/ice-servers")
                            .param("userId", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.iceServers").isArray());
        }

        @Test
        @DisplayName("roomId만")
        void t4() throws Exception {
            // given
            Long roomId = 100L;

            // when & then
            mockMvc.perform(get("/api/webrtc/ice-servers")
                            .param("roomId", roomId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.iceServers").isArray());
        }

        @Test
        @DisplayName("Google STUN 서버 포함")
        void t5() throws Exception {
            // when & then
            MvcResult result = mockMvc.perform(get("/api/webrtc/ice-servers")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();

            // Google STUN 서버 확인
            assertThat(content).contains("stun.l.google.com");
        }

        @Test
        @DisplayName("응답 구조 검증")
        void t6() throws Exception {
            // when & then
            MvcResult result = mockMvc.perform(get("/api/webrtc/ice-servers")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            assertThat(content).isNotEmpty();

            // JSON 구조 검증
            assertThat(content).contains("code");
            assertThat(content).contains("message");
            assertThat(content).contains("data");
            assertThat(content).contains("success");
            assertThat(content).contains("iceServers");
        }
    }

    @Nested
    @DisplayName("Health Check")
    class HealthCheckTest {

        @Test
        @DisplayName("정상 응답")
        void t7() throws Exception {
            // when & then
            mockMvc.perform(get("/api/webrtc/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                    .andExpect(jsonPath("$.message").value("WebRTC 서비스 정상 작동 중"))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("반복 호출")
        void t8() throws Exception {
            // when & then
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/webrtc/health")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("응답 구조 검증")
        void t9() throws Exception {
            // when & then
            MvcResult result = mockMvc.perform(get("/api/webrtc/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();

            // RsData 구조 검증
            assertThat(content).contains("code");
            assertThat(content).contains("message");
            assertThat(content).contains("success");
        }
    }

    @Nested
    @DisplayName("엔드포인트")
    class EndpointTest {

        @Test
        @DisplayName("Content-Type 생략 가능")
        void t10() throws Exception {
            // when & then
            mockMvc.perform(get("/api/webrtc/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("JSON 응답")
        void t11() throws Exception {
            // ICE servers endpoint
            mockMvc.perform(get("/api/webrtc/ice-servers"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            // Health endpoint
            mockMvc.perform(get("/api/webrtc/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }
}