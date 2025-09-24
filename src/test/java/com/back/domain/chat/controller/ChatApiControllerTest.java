package com.back.domain.chat.controller;

import com.back.domain.chat.dto.ChatPageResponse;
import com.back.domain.chat.service.ChatService;
import com.back.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
class ChatApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ChatService chatService;

    @Test
    @DisplayName("채팅 기록 조회 성공")
    void t1() throws Exception {
        ChatPageResponse mockResponse = ChatPageResponse.builder()
                .content(List.of())
                .pageable(ChatPageResponse.PageableDto.builder()
                        .page(0)
                        .size(20)
                        .hasNext(false)
                        .build())
                .totalElements(0)
                .build();

        given(chatService.getRoomChatHistory(anyLong(), anyInt(), anyInt(), any()))
                .willReturn(mockResponse);

        // JWT 관련 스텁
        given(jwtTokenProvider.validateToken(anyString())).willReturn(true);
        given(jwtTokenProvider.getAuthentication(anyString()))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        "mockUser", null, List.of())
                );

        mockMvc.perform(get("/api/rooms/1/messages")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer faketoken") // 가짜 토큰 넣기
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}

