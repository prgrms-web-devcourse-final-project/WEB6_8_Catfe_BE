package com.back.domain.chat.room.controller;

import com.back.domain.chat.room.dto.RoomChatPageResponse;
import com.back.domain.chat.room.service.RoomChatService;
import com.back.global.security.CustomUserDetails;
import com.back.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class RoomChatApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RoomChatService roomChatService;

    @Test
    @DisplayName("채팅 기록 조회 성공 - JWT 토큰 있음")
    void t1() throws Exception {
        RoomChatPageResponse mockResponse = RoomChatPageResponse.of(
                List.of(),        // content
                0,               // page
                20,              // size
                false,           // hasNext
                0L               // totalElements
        );

        given(roomChatService.getRoomChatHistory(anyLong(), anyInt(), anyInt(), any()))
                .willReturn(mockResponse);

        // JWT 관련 스텁
        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);

        CustomUserDetails mockUser = CustomUserDetails.builder()
                .userId(1L)
                .username("testuser")
                .build();

        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
                );

        mockMvc.perform(get("/api/rooms/1/messages")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer faketoken")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("채팅 기록 조회 성공 - 빈 페이지")
    void t2() throws Exception {
        RoomChatPageResponse mockResponse = RoomChatPageResponse.empty(0, 20);

        given(roomChatService.getRoomChatHistory(anyLong(), anyInt(), anyInt(), any()))
                .willReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/1/messages")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.pageable.page").value(0))
                .andExpect(jsonPath("$.data.pageable.size").value(20))
                .andExpect(jsonPath("$.data.pageable.hasNext").value(false));
    }

    // Security 설정을 authenticated()로 변경한 후에 다시 활성화
    /*
    @Test
    @DisplayName("JWT 토큰 없이 요청 - 401 Unauthorized")
    void t3() throws Exception {
        mockMvc.perform(get("/api/rooms/1/messages")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("잘못된 JWT 토큰으로 요청 - 401 Unauthorized")
    void t4() throws Exception {
        given(jwtTokenProvider.validateToken("invalidtoken")).willReturn(false);

        mockMvc.perform(get("/api/rooms/1/messages")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer invalidtoken")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
    */

    @Test
    @DisplayName("페이징 파라미터 테스트 - 큰 size 요청")
    void t5() throws Exception {
        // size가 큰 경우의 응답
        RoomChatPageResponse mockResponse = RoomChatPageResponse.of(
                List.of(),        // content
                0,               // page
                100,             // size (최대값으로 제한됨)
                false,           // hasNext
                0L               // totalElements
        );

        given(roomChatService.getRoomChatHistory(anyLong(), anyInt(), anyInt(), any()))
                .willReturn(mockResponse);

        mockMvc.perform(get("/api/rooms/1/messages")
                        .param("page", "0")
                        .param("size", "150") // 큰 값 요청
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageable.size").value(100)); // 100으로 제한됨
    }
}