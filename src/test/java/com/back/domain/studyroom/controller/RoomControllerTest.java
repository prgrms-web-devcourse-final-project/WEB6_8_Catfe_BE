package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.*;
import com.back.domain.studyroom.entity.*;
import com.back.domain.studyroom.service.RoomService;
import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@DisplayName("RoomController 테스트")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    private User testUser;
    private Room testRoom;
    private RoomMember testMember;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .password("password123")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        
        // UserProfile 설정 (nickname을 위해)
        UserProfile userProfile = new UserProfile();
        userProfile.setNickname("테스트유저");
        testUser.setUserProfile(userProfile);

        // 테스트 방 생성
        testRoom = Room.create(
                "테스트 방",
                "테스트 설명",
                false,
                null,
                10,
                testUser,
                null
        );

        // 테스트 멤버 생성
        testMember = RoomMember.createHost(testRoom, testUser);
    }

    @Test
    @DisplayName("방 생성 API 테스트")
    void createRoom() throws Exception {
        // given
        CreateRoomRequest request = new CreateRoomRequest(
                "테스트 방",
                "테스트 설명",
                false,
                null,
                10
        );

        given(roomService.createRoom(
                anyString(),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                anyLong()
        )).willReturn(testRoom);

        // when & then
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("방 생성 완료"))
                .andExpect(jsonPath("$.data.title").value("테스트 방"));

        verify(roomService, times(1)).createRoom(
                anyString(),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                anyLong()
        );
    }

    @Test
    @DisplayName("방 생성 API - Validation 실패")
    void createRoom_ValidationFail() throws Exception {
        // given
        CreateRoomRequest request = new CreateRoomRequest(
                "", // 빈 제목
                "테스트 설명",
                false,
                null,
                10
        );

        // when & then
        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("방 입장 API 테스트")
    void joinRoom() throws Exception {
        // given
        given(roomService.joinRoom(anyLong(), any(), anyLong())).willReturn(testMember);

        // when & then
        mockMvc.perform(post("/api/rooms/1/join")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("방 입장 완료"))
                .andExpect(jsonPath("$.data.roomId").exists());

        verify(roomService, times(1)).joinRoom(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("방 나가기 API 테스트")
    void leaveRoom() throws Exception {
        // given
        // when & then
        mockMvc.perform(post("/api/rooms/1/leave")
                        .header("Authorization", "Bearer test-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("방 퇴장 완료"));

        verify(roomService, times(1)).leaveRoom(anyLong(), anyLong());
    }

    @Test
    @DisplayName("공개 방 목록 조회 API 테스트")
    void getRooms() throws Exception {
        // given
        Page<Room> roomPage = new PageImpl<>(
                Arrays.asList(testRoom),
                PageRequest.of(0, 20),
                1
        );
        given(roomService.getJoinableRooms(any())).willReturn(roomPage);

        // when & then
        mockMvc.perform(get("/api/rooms")
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.rooms").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(roomService, times(1)).getJoinableRooms(any());
    }

    @Test
    @DisplayName("방 상세 정보 조회 API 테스트")
    void getRoomDetail() throws Exception {
        // given
        given(roomService.getRoomDetail(anyLong(), anyLong())).willReturn(testRoom);
        given(roomService.getRoomMembers(anyLong(), anyLong())).willReturn(Arrays.asList(testMember));

        // when & then
        mockMvc.perform(get("/api/rooms/1")
                        .header("Authorization", "Bearer test-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.roomId").exists())
                .andExpect(jsonPath("$.data.title").value("테스트 방"))
                .andExpect(jsonPath("$.data.members").isArray());

        verify(roomService, times(1)).getRoomDetail(anyLong(), anyLong());
        verify(roomService, times(1)).getRoomMembers(anyLong(), anyLong());
    }

    @Test
    @DisplayName("내 참여 방 목록 조회 API 테스트")
    void getMyRooms() throws Exception {
        // given
        given(roomService.getUserRooms(anyLong())).willReturn(Arrays.asList(testRoom));
        given(roomService.getUserRoomRole(anyLong(), anyLong())).willReturn(RoomRole.HOST);

        // when & then
        mockMvc.perform(get("/api/rooms/my")
                        .header("Authorization", "Bearer test-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("테스트 방"))
                .andExpect(jsonPath("$.data[0].myRole").value("HOST"));

        verify(roomService, times(1)).getUserRooms(anyLong());
    }

    @Test
    @DisplayName("방 설정 수정 API 테스트")
    void updateRoom() throws Exception {
        // given
        UpdateRoomSettingsRequest request = new UpdateRoomSettingsRequest(
                "변경된 제목",
                "변경된 설명",
                15,
                true,
                true,
                false
        );

        // when & then
        mockMvc.perform(put("/api/rooms/1")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("방 설정 변경 완료"));

        verify(roomService, times(1)).updateRoomSettings(
                anyLong(),
                anyString(),
                anyString(),
                anyInt(),
                anyBoolean(),
                anyBoolean(),
                anyBoolean(),
                anyLong()
        );
    }

    @Test
    @DisplayName("방 종료 API 테스트")
    void deleteRoom() throws Exception {
        // given
        // when & then
        mockMvc.perform(delete("/api/rooms/1")
                        .header("Authorization", "Bearer test-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("방 종료 완료"));

        verify(roomService, times(1)).terminateRoom(anyLong(), anyLong());
    }

    @Test
    @DisplayName("방 멤버 목록 조회 API 테스트")
    void getRoomMembers() throws Exception {
        // given
        given(roomService.getRoomMembers(anyLong(), anyLong())).willReturn(Arrays.asList(testMember));

        // when & then
        mockMvc.perform(get("/api/rooms/1/members")
                        .header("Authorization", "Bearer test-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nickname").value("테스트유저"));

        verify(roomService, times(1)).getRoomMembers(anyLong(), anyLong());
    }

    @Test
    @DisplayName("인기 방 목록 조회 API 테스트")
    void getPopularRooms() throws Exception {
        // given
        Page<Room> roomPage = new PageImpl<>(
                Arrays.asList(testRoom),
                PageRequest.of(0, 20),
                1
        );
        given(roomService.getPopularRooms(any())).willReturn(roomPage);

        // when & then
        mockMvc.perform(get("/api/rooms/popular")
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.rooms").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(roomService, times(1)).getPopularRooms(any());
    }
}
