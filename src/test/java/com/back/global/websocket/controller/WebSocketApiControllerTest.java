package com.back.global.websocket.controller;

import com.back.global.websocket.service.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("WebSocket REST API 컨트롤러")
class WebSocketApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebSocketSessionManager sessionManager;

    @Nested
    @DisplayName("헬스체크")
    class HealthCheckTest {

        @Test
        @DisplayName("정상 - 서비스 상태 확인")
        void t1() throws Exception {
            // given
            long totalOnlineUsers = 5L;
            given(sessionManager.getTotalOnlineUserCount()).willReturn(totalOnlineUsers);

            // when & then
            mockMvc.perform(get("/api/ws/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                    .andExpect(jsonPath("$.message").value("WebSocket 서비스가 정상 동작중입니다."))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.service").value("WebSocket"))
                    .andExpect(jsonPath("$.data.status").value("running"))
                    .andExpect(jsonPath("$.data.timestamp").exists())
                    .andExpect(jsonPath("$.data.sessionTTL").value("10분 (Heartbeat 방식)"))
                    .andExpect(jsonPath("$.data.heartbeatInterval").value("5분"))
                    .andExpect(jsonPath("$.data.totalOnlineUsers").value(totalOnlineUsers))
                    .andExpect(jsonPath("$.data.endpoints").exists())
                    .andExpect(jsonPath("$.data.endpoints.websocket").value("/ws"))
                    .andExpect(jsonPath("$.data.endpoints.heartbeat").value("/app/heartbeat"))
                    .andExpect(jsonPath("$.data.endpoints.activity").value("/app/activity"));

            verify(sessionManager).getTotalOnlineUserCount();
        }

        @Test
        @DisplayName("정상 - 온라인 유저 0명")
        void t2() throws Exception {
            // given
            given(sessionManager.getTotalOnlineUserCount()).willReturn(0L);

            // when & then
            mockMvc.perform(get("/api/ws/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalOnlineUsers").value(0));

            verify(sessionManager).getTotalOnlineUserCount();
        }

        @Test
        @DisplayName("정상 - 응답 구조 검증")
        void t3() throws Exception {
            // given
            given(sessionManager.getTotalOnlineUserCount()).willReturn(10L);

            // when & then
            MvcResult result = mockMvc.perform(get("/api/ws/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();

            assertThat(content).contains("service");
            assertThat(content).contains("status");
            assertThat(content).contains("timestamp");
            assertThat(content).contains("sessionTTL");
            assertThat(content).contains("heartbeatInterval");
            assertThat(content).contains("totalOnlineUsers");
            assertThat(content).contains("endpoints");
        }

        @Test
        @DisplayName("정상 - 엔드포인트 정보 포함")
        void t4() throws Exception {
            // given
            given(sessionManager.getTotalOnlineUserCount()).willReturn(3L);

            // when & then
            mockMvc.perform(get("/api/ws/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.endpoints.websocket").exists())
                    .andExpect(jsonPath("$.data.endpoints.heartbeat").exists())
                    .andExpect(jsonPath("$.data.endpoints.activity").exists())
                    .andExpect(jsonPath("$.data.endpoints.joinRoom").exists())
                    .andExpect(jsonPath("$.data.endpoints.leaveRoom").exists());
        }

        @Test
        @DisplayName("정상 - Content-Type 없이도 동작")
        void t5() throws Exception {
            // given
            given(sessionManager.getTotalOnlineUserCount()).willReturn(1L);

            // when & then
            mockMvc.perform(get("/api/ws/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("연결 정보 조회")
    class ConnectionInfoTest {

        @Test
        @DisplayName("정상 - 연결 정보 조회")
        void t6() throws Exception {
            // when & then
            mockMvc.perform(get("/api/ws/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                    .andExpect(jsonPath("$.message").value("WebSocket 연결 정보"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.websocketUrl").value("/ws"))
                    .andExpect(jsonPath("$.data.sockjsSupport").value(true))
                    .andExpect(jsonPath("$.data.stompVersion").value("1.2"))
                    .andExpect(jsonPath("$.data.heartbeatInterval").value("5분"))
                    .andExpect(jsonPath("$.data.sessionTTL").value("10분"));
        }

        @Test
        @DisplayName("정상 - 구독 토픽 정보 포함")
        void t7() throws Exception {
            // when & then
            mockMvc.perform(get("/api/ws/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.subscribeTopics").exists())
                    .andExpect(jsonPath("$.data.subscribeTopics.roomChat").value("/topic/rooms/{roomId}/chat"))
                    .andExpect(jsonPath("$.data.subscribeTopics.privateMessage").value("/user/queue/messages"))
                    .andExpect(jsonPath("$.data.subscribeTopics.notifications").value("/user/queue/notifications"));
        }

        @Test
        @DisplayName("정상 - 전송 목적지 정보 포함")
        void t8() throws Exception {
            // when & then
            mockMvc.perform(get("/api/ws/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sendDestinations").exists())
                    .andExpect(jsonPath("$.data.sendDestinations.heartbeat").value("/app/heartbeat"))
                    .andExpect(jsonPath("$.data.sendDestinations.activity").value("/app/activity"))
                    .andExpect(jsonPath("$.data.sendDestinations.roomChat").value("/app/rooms/{roomId}/chat"));
        }

        @Test
        @DisplayName("정상 - 응답 구조 검증")
        void t9() throws Exception {
            // when & then
            MvcResult result = mockMvc.perform(get("/api/ws/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();

            assertThat(content).contains("websocketUrl");
            assertThat(content).contains("sockjsSupport");
            assertThat(content).contains("stompVersion");
            assertThat(content).contains("heartbeatInterval");
            assertThat(content).contains("sessionTTL");
            assertThat(content).contains("subscribeTopics");
            assertThat(content).contains("sendDestinations");
        }

        @Test
        @DisplayName("정상 - SockJS 지원 확인")
        void t10() throws Exception {
            // when & then
            mockMvc.perform(get("/api/ws/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sockjsSupport").value(true))
                    .andExpect(jsonPath("$.data.stompVersion").value("1.2"));
        }

        @Test
        @DisplayName("정상 - Content-Type 없이도 동작")
        void t11() throws Exception {
            // when & then
            mockMvc.perform(get("/api/ws/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("API 엔드포인트")
    class EndpointTest {

        @Test
        @DisplayName("정상 - 모든 엔드포인트 JSON 응답")
        void t12() throws Exception {
            // given
            given(sessionManager.getTotalOnlineUserCount()).willReturn(0L);

            // when & then
            mockMvc.perform(get("/api/ws/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            mockMvc.perform(get("/api/ws/info"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("정상 - RsData 구조 일관성")
        void t13() throws Exception {
            // given
            given(sessionManager.getTotalOnlineUserCount()).willReturn(0L);

            // when & then - health
            mockMvc.perform(get("/api/ws/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.success").exists())
                    .andExpect(jsonPath("$.data").exists());

            // when & then - info
            mockMvc.perform(get("/api/ws/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.success").exists())
                    .andExpect(jsonPath("$.data").exists());
        }
    }
}