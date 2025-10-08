package com.back.domain.chat.room.controller;

import com.back.domain.chat.room.dto.ChatClearedNotification;
import com.back.domain.chat.room.dto.RoomChatPageResponse;
import com.back.domain.chat.room.service.RoomChatService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.security.user.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

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
        given(jwtTokenProvider.validateAccessToken("invalidtoken")).willReturn(false);

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

    @Test
    @DisplayName("채팅 전체 삭제 성공 - 방장 권한")
    void t6() throws Exception {
        Long roomId = 1L;
        Long userId = 1L;
        int messageCount = 15;

        // Mock 설정
        ChatClearedNotification.ClearedByDto clearedByInfo = new ChatClearedNotification.ClearedByDto(
                userId, "방장", "https://example.com/profile.jpg", "HOST"
        );

        given(roomChatService.getRoomChatCount(roomId)).willReturn(messageCount);
        given(roomChatService.clearRoomChat(roomId, userId)).willReturn(clearedByInfo);

        // JWT 관련 스텁
        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);

        CustomUserDetails mockUser = CustomUserDetails.builder()
                .userId(userId)
                .username("방장")
                .build();

        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
                );

        String requestBody = """
                {
                    "confirmMessage": "모든 채팅을 삭제하겠습니다"
                }
                """;

        mockMvc.perform(delete("/api/rooms/{roomId}/messages", roomId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("채팅 메시지 일괄 삭제 완료"))
                .andExpect(jsonPath("$.data.roomId").value(roomId))
                .andExpect(jsonPath("$.data.deletedCount").value(messageCount))
                .andExpect(jsonPath("$.data.clearedBy.userId").value(userId))
                .andExpect(jsonPath("$.data.clearedBy.nickname").value("방장"))
                .andExpect(jsonPath("$.data.clearedBy.role").value("HOST"));

        // WebSocket 메시지가 전송되었는지 확인
        verify(messagingTemplate).convertAndSend(
                eq("/topic/room/" + roomId + "/chat-cleared"),
                any(ChatClearedNotification.class)
        );
    }

    @Test
    @DisplayName("채팅 전체 삭제 성공 - 부방장 권한")
    void t7() throws Exception {
        Long roomId = 2L;
        Long userId = 2L;
        int messageCount = 8;

        ChatClearedNotification.ClearedByDto clearedByInfo = new ChatClearedNotification.ClearedByDto(
                userId, "부방장", "https://example.com/sub-host.jpg", "SUB_HOST"
        );

        given(roomChatService.getRoomChatCount(roomId)).willReturn(messageCount);
        given(roomChatService.clearRoomChat(roomId, userId)).willReturn(clearedByInfo);

        // JWT 관련 스텁
        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);

        CustomUserDetails mockUser = CustomUserDetails.builder()
                .userId(userId)
                .username("부방장")
                .build();

        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
                );

        String requestBody = """
                {
                    "confirmMessage": "모든 채팅을 삭제하겠습니다"
                }
                """;

        mockMvc.perform(delete("/api/rooms/{roomId}/messages", roomId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clearedBy.role").value("SUB_HOST"));
    }

    @Test
    @DisplayName("채팅 전체 삭제 실패 - 권한 없음 (일반 멤버)")
    void t8() throws Exception {
        Long roomId = 1L;
        Long userId = 3L;

        willThrow(new CustomException(ErrorCode.CHAT_DELETE_FORBIDDEN))
                .given(roomChatService).clearRoomChat(roomId, userId);

        // JWT 관련 스텁
        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);

        CustomUserDetails mockUser = CustomUserDetails.builder()
                .userId(userId)
                .username("일반멤버")
                .build();

        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        String requestBody = """
                {
                    "confirmMessage": "모든 채팅을 삭제하겠습니다"
                }
                """;

        mockMvc.perform(delete("/api/rooms/{roomId}/messages", roomId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_014"))
                .andExpect(jsonPath("$.message").value("채팅 삭제 권한이 없습니다. 방장 또는 부방장만 가능합니다."));
    }

    @Test
    @DisplayName("채팅 전체 삭제 실패 - 존재하지 않는 방")
    void t9() throws Exception {
        Long nonExistentRoomId = 999L;
        Long userId = 1L;

        willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND))
                .given(roomChatService).clearRoomChat(nonExistentRoomId, userId);

        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);

        CustomUserDetails mockUser = CustomUserDetails.builder()
                .userId(userId)
                .username("방장")
                .build();

        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        String requestBody = """
                {
                    "confirmMessage": "모든 채팅을 삭제하겠습니다"
                }
                """;

        mockMvc.perform(delete("/api/rooms/{roomId}/messages", nonExistentRoomId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 방입니다."));
    }

    @Test
    @DisplayName("채팅 전체 삭제 실패 - 잘못된 확인 메시지")
    void t10() throws Exception {
        Long roomId = 1L;
        Long userId = 1L;

        willThrow(new CustomException(ErrorCode.INVALID_DELETE_CONFIRMATION))
                .given(roomChatService).clearRoomChat(roomId, userId);

        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);

        CustomUserDetails mockUser = CustomUserDetails.builder()
                .userId(userId)
                .username("방장")
                .build();

        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        String requestBody = """
                {
                    "confirmMessage": "잘못된 확인 메시지"
                }
                """;

        mockMvc.perform(delete("/api/rooms/{roomId}/messages", roomId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ROOM_015"))
                .andExpect(jsonPath("$.message").value("삭제 확인 메시지가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("채팅 전체 삭제 실패 - 빈 확인 메시지")
    void t11() throws Exception {
        Long roomId = 1L;

        // JWT 관련 스텁
        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);

        CustomUserDetails mockUser = CustomUserDetails.builder()
                .userId(1L)
                .username("방장")
                .build();

        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
                );

        String requestBody = """
                {
                    "confirmMessage": ""
                }
                """;

        mockMvc.perform(delete("/api/rooms/{roomId}/messages", roomId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("채팅 전체 삭제 실패 - 방 멤버가 아님")
    void t12() throws Exception {
        Long roomId = 1L;
        Long userId = 5L;

        willThrow(new CustomException(ErrorCode.NOT_ROOM_MEMBER))
                .given(roomChatService).clearRoomChat(roomId, userId);

        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);

        CustomUserDetails mockUser = CustomUserDetails.builder()
                .userId(userId)
                .username("외부사용자")
                .build();

        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        String requestBody = """
                {
                    "confirmMessage": "모든 채팅을 삭제하겠습니다"
                }
                """;

        mockMvc.perform(delete("/api/rooms/{roomId}/messages", roomId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_009"))
                .andExpect(jsonPath("$.message").value("방 멤버가 아닙니다."));
    }
}