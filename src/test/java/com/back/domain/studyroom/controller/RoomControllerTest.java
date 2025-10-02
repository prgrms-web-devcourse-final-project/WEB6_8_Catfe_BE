package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.*;
import com.back.domain.studyroom.entity.*;
import com.back.domain.studyroom.service.RoomService;
import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomController 테스트 - JWT 인증 통합")
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private RoomController roomController;

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
        
        // UserProfile 설정
        UserProfile userProfile = new UserProfile();
        userProfile.setNickname("테스트유저");
        testUser.setUserProfile(userProfile);

        // 테스트 방 생성 (WebRTC 사용)
        testRoom = Room.create(
                "테스트 방",
                "테스트 설명",
                false,
                null,
                10,
                testUser,
                null,
                true  // useWebRTC
        );

        // 테스트 멤버 생성
        testMember = RoomMember.createHost(testRoom, testUser);

        // CurrentUser Mock 설정은 각 테스트에서 필요할 때만 설정
    }

    @Test
    @DisplayName("방 생성 API 테스트 - JWT 인증")
    void createRoom() {
        // given
        given(currentUser.getUserId()).willReturn(1L);

        CreateRoomRequest request = new CreateRoomRequest(
                "테스트 방",
                "테스트 설명",
                false,
                null,
                10,
                true  // useWebRTC
        );

        given(roomService.createRoom(
                anyString(),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                eq(1L),
                anyBoolean()  // useWebRTC 파라미터 추가
        )).willReturn(testRoom);
        
        RoomResponse roomResponse = RoomResponse.from(testRoom, 1);
        given(roomService.toRoomResponse(any(Room.class))).willReturn(roomResponse);

        // when
        ResponseEntity<RsData<RoomResponse>> response = roomController.createRoom(request);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getTitle()).isEqualTo("테스트 방");

        verify(currentUser, times(1)).getUserId();
        verify(roomService, times(1)).createRoom(
                anyString(),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                eq(1L),
                anyBoolean()  // useWebRTC 파라미터 추가
        );
        verify(roomService, times(1)).toRoomResponse(any(Room.class));
    }

    @Test
    @DisplayName("방 입장 API 테스트 - JWT 인증")
    void joinRoom() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        
        JoinRoomRequest request = new JoinRoomRequest(null);
        given(roomService.joinRoom(eq(1L), any(), eq(1L))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = roomController.joinRoom(1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(currentUser, times(1)).getUserId();
        verify(roomService, times(1)).joinRoom(eq(1L), any(), eq(1L));
    }

    @Test
    @DisplayName("방 나가기 API 테스트 - JWT 인증")
    void leaveRoom() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        
        // when
        ResponseEntity<RsData<Void>> response = roomController.leaveRoom(1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(currentUser, times(1)).getUserId();
        verify(roomService, times(1)).leaveRoom(eq(1L), eq(1L));
    }

    @Test
    @DisplayName("공개 방 목록 조회 API 테스트")
    void getRooms() {
        // given
        Page<Room> roomPage = new PageImpl<>(
                Arrays.asList(testRoom),
                PageRequest.of(0, 20),
                1
        );
        given(roomService.getJoinableRooms(any())).willReturn(roomPage);
        
        List<RoomResponse> roomResponses = Arrays.asList(RoomResponse.from(testRoom, 1));
        given(roomService.toRoomResponseList(anyList())).willReturn(roomResponses);

        // when
        ResponseEntity<RsData<Map<String, Object>>> response = roomController.getRooms(0, 20);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().get("rooms")).isNotNull();
        
        verify(roomService, times(1)).getJoinableRooms(any());
        verify(roomService, times(1)).toRoomResponseList(anyList());
    }

    @Test
    @DisplayName("방 상세 정보 조회 API 테스트 - JWT 인증")
    void getRoomDetail() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        
        given(roomService.getRoomDetail(eq(1L), eq(1L))).willReturn(testRoom);
        given(roomService.getRoomMembers(eq(1L), eq(1L))).willReturn(Arrays.asList(testMember));
        
        RoomDetailResponse roomDetailResponse = RoomDetailResponse.of(
            testRoom, 
            1, 
            Arrays.asList(RoomMemberResponse.from(testMember))
        );
        given(roomService.toRoomDetailResponse(any(Room.class), anyList())).willReturn(roomDetailResponse);

        // when
        ResponseEntity<RsData<RoomDetailResponse>> response = roomController.getRoomDetail(1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getTitle()).isEqualTo("테스트 방");

        verify(currentUser, times(1)).getUserId();
        verify(roomService, times(1)).getRoomDetail(eq(1L), eq(1L));
        verify(roomService, times(1)).getRoomMembers(eq(1L), eq(1L));
        verify(roomService, times(1)).toRoomDetailResponse(any(Room.class), anyList());
    }

    @Test
    @DisplayName("내 참여 방 목록 조회 API 테스트 - JWT 인증")
    void getMyRooms() {
        // given
        given(currentUser.getUserId()).willReturn(1L);

        // Room에 ID 설정 (리플렉션 사용)
        try {
            java.lang.reflect.Field idField = testRoom.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testRoom, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(roomService.getUserRooms(eq(1L))).willReturn(Arrays.asList(testRoom));
        
        List<MyRoomResponse> myRoomResponses = Arrays.asList(
            MyRoomResponse.of(testRoom, 1, RoomRole.HOST)
        );
        given(roomService.toMyRoomResponseList(anyList(), eq(1L))).willReturn(myRoomResponses);

        // when
        ResponseEntity<RsData<List<MyRoomResponse>>> response = roomController.getMyRooms();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getTitle()).isEqualTo("테스트 방");

        verify(currentUser, times(1)).getUserId();
        verify(roomService, times(1)).getUserRooms(eq(1L));
        verify(roomService, times(1)).toMyRoomResponseList(anyList(), eq(1L));
    }

    @Test
    @DisplayName("방 설정 수정 API 테스트 - JWT 인증")
    void updateRoom() {
        // given
        given(currentUser.getUserId()).willReturn(1L);

        UpdateRoomSettingsRequest request = new UpdateRoomSettingsRequest(
                "변경된 제목",
                "변경된 설명",
                15,
                true,
                true,
                false
        );

        // when
        ResponseEntity<RsData<Void>> response = roomController.updateRoom(1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(currentUser, times(1)).getUserId();
        verify(roomService, times(1)).updateRoomSettings(
                eq(1L),
                anyString(),
                anyString(),
                anyInt(),
                anyBoolean(),
                anyBoolean(),
                anyBoolean(),
                eq(1L)
        );
    }

    @Test
    @DisplayName("방 종료 API 테스트 - JWT 인증")
    void deleteRoom() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        
        // when
        ResponseEntity<RsData<Void>> response = roomController.deleteRoom(1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(currentUser, times(1)).getUserId();
        verify(roomService, times(1)).terminateRoom(eq(1L), eq(1L));
    }

    @Test
    @DisplayName("방 멤버 목록 조회 API 테스트 - JWT 인증")
    void getRoomMembers() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        
        given(roomService.getRoomMembers(eq(1L), eq(1L))).willReturn(Arrays.asList(testMember));

        // when
        ResponseEntity<RsData<List<RoomMemberResponse>>> response = roomController.getRoomMembers(1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getNickname()).isEqualTo("테스트유저");

        verify(currentUser, times(1)).getUserId();
        verify(roomService, times(1)).getRoomMembers(eq(1L), eq(1L));
    }

    @Test
    @DisplayName("인기 방 목록 조회 API 테스트")
    void getPopularRooms() {
        // given
        Page<Room> roomPage = new PageImpl<>(
                Arrays.asList(testRoom),
                PageRequest.of(0, 20),
                1
        );
        given(roomService.getPopularRooms(any())).willReturn(roomPage);
        
        List<RoomResponse> roomResponses = Arrays.asList(RoomResponse.from(testRoom, 1));
        given(roomService.toRoomResponseList(anyList())).willReturn(roomResponses);

        // when
        ResponseEntity<RsData<Map<String, Object>>> response = roomController.getPopularRooms(0, 20);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().get("rooms")).isNotNull();
        
        verify(roomService, times(1)).getPopularRooms(any());
        verify(roomService, times(1)).toRoomResponseList(anyList());
    }

    @Test
    @DisplayName("방 생성 API - WebRTC 활성화 테스트")
    void createRoom_WithWebRTC() {
        // given
        given(currentUser.getUserId()).willReturn(1L);

        CreateRoomRequest request = new CreateRoomRequest(
                "WebRTC 방",
                "화상 채팅 가능",
                false,
                null,
                10,
                true  // WebRTC 활성화
        );

        Room webRTCRoom = Room.create(
                "WebRTC 방",
                "화상 채팅 가능",
                false,
                null,
                10,
                testUser,
                null,
                true
        );

        given(roomService.createRoom(
                anyString(),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                eq(1L),
                eq(true)  // WebRTC true 검증
        )).willReturn(webRTCRoom);
        
        RoomResponse roomResponse = RoomResponse.from(webRTCRoom, 1);
        given(roomService.toRoomResponse(any(Room.class))).willReturn(roomResponse);

        // when
        ResponseEntity<RsData<RoomResponse>> response = roomController.createRoom(request);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getAllowCamera()).isTrue();
        assertThat(response.getBody().getData().getAllowAudio()).isTrue();
        assertThat(response.getBody().getData().getAllowScreenShare()).isTrue();

        verify(roomService, times(1)).createRoom(
                anyString(),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                eq(1L),
                eq(true)
        );
    }

    @Test
    @DisplayName("방 생성 API - WebRTC 비활성화 테스트")
    void createRoom_WithoutWebRTC() {
        // given
        given(currentUser.getUserId()).willReturn(1L);

        CreateRoomRequest request = new CreateRoomRequest(
                "채팅 전용 방",
                "텍스트만 가능",
                false,
                null,
                50,
                false  // WebRTC 비활성화
        );

        Room chatOnlyRoom = Room.create(
                "채팅 전용 방",
                "텍스트만 가능",
                false,
                null,
                50,
                testUser,
                null,
                false
        );

        given(roomService.createRoom(
                anyString(),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                eq(1L),
                eq(false)  // WebRTC false 검증
        )).willReturn(chatOnlyRoom);
        
        RoomResponse roomResponse = RoomResponse.from(chatOnlyRoom, 1);
        given(roomService.toRoomResponse(any(Room.class))).willReturn(roomResponse);

        // when
        ResponseEntity<RsData<RoomResponse>> response = roomController.createRoom(request);
        
        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getAllowCamera()).isFalse();
        assertThat(response.getBody().getData().getAllowAudio()).isFalse();
        assertThat(response.getBody().getData().getAllowScreenShare()).isFalse();

        verify(roomService, times(1)).createRoom(
                anyString(),
                anyString(),
                anyBoolean(),
                any(),
                anyInt(),
                eq(1L),
                eq(false)
        );
    }
}
