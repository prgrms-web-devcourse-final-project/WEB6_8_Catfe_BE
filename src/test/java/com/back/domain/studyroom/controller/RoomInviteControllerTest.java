package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.InviteCodeResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomInviteCode;
import com.back.domain.studyroom.service.RoomInviteService;
import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.global.common.dto.RsData;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomInviteController 테스트")
class RoomInviteControllerTest {

    @Mock
    private RoomInviteService inviteService;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private RoomInviteController inviteController;

    private User testUser;
    private User testUser2;
    private Room testRoom;
    private Room privateRoom;
    private RoomInviteCode testInviteCode;
    private RoomInviteCode privateRoomInviteCode;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 1 생성
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .password("password123")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        UserProfile userProfile = new UserProfile();
        userProfile.setNickname("테스트유저");
        testUser.setUserProfile(userProfile);

        // 테스트 사용자 2 생성
        testUser2 = User.builder()
                .id(2L)
                .username("testuser2")
                .email("test2@test.com")
                .password("password456")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        UserProfile userProfile2 = new UserProfile();
        userProfile2.setNickname("테스트유저2");
        testUser2.setUserProfile(userProfile2);

        // 공개 방 생성
        testRoom = Room.create(
                "테스트 방",
                "테스트 설명",
                false,
                null,
                10,
                testUser,
                null,
                true,
                null
        );

        setRoomId(testRoom, 1L);

        // 비공개 방 생성
        privateRoom = Room.create(
                "비밀 방",
                "비밀 설명",
                true,
                "1234",
                10,
                testUser,
                null,
                true,
                null
        );

        setRoomId(privateRoom, 2L);

        // 공개 방 초대 코드 생성
        testInviteCode = RoomInviteCode.create("ABC12345", testRoom, testUser);

        // 비공개 방 초대 코드 생성
        privateRoomInviteCode = RoomInviteCode.create("PRIVATE1", privateRoom, testUser);
    }

    private void setRoomId(Room room, Long id) {
        try {
            java.lang.reflect.Field idField = room.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(room, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ====================== 내 초대 코드 조회/생성 테스트 ======================

    @Test
    @DisplayName("내 초대 코드 조회 - 성공")
    void getMyInviteCode_Success() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("초대 코드 조회 완료");
        
        InviteCodeResponse data = response.getBody().getData();
        assertThat(data.getInviteCode()).isEqualTo("ABC12345");
        assertThat(data.getRoomId()).isEqualTo(1L);
        assertThat(data.isValid()).isTrue();

        verify(currentUser, times(1)).getUserId();
        verify(inviteService, times(1)).getOrCreateMyInviteCode(1L, 1L);
    }

    @Test
    @DisplayName("내 초대 코드 조회 - 비공개 방도 가능")
    void getMyInviteCode_Success_PrivateRoom() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(2L, 1L)).willReturn(privateRoomInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(2L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        
        InviteCodeResponse data = response.getBody().getData();
        assertThat(data.getInviteCode()).isEqualTo("PRIVATE1");
        assertThat(data.getRoomId()).isEqualTo(2L);

        verify(inviteService, times(1)).getOrCreateMyInviteCode(2L, 1L);
    }

    @Test
    @DisplayName("초대 링크 URL 형식 확인")
    void inviteLink_Format() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getInviteLink())
                .isEqualTo("https://catfe.com/invite/ABC12345");
    }

    @Test
    @DisplayName("초대 코드 응답에 필수 정보 포함 확인")
    void inviteCodeResponse_ContainsRequiredInfo() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getBody()).isNotNull();
        InviteCodeResponse data = response.getBody().getData();

        assertThat(data.getInviteCode()).isNotNull();
        assertThat(data.getInviteLink()).isNotNull();
        assertThat(data.getRoomId()).isNotNull();
        assertThat(data.getRoomTitle()).isNotNull();
        assertThat(data.getCreatedByNickname()).isNotNull();
        assertThat(data.getExpiresAt()).isNotNull();
        assertThat(data.isActive()).isTrue();
        assertThat(data.isValid()).isTrue();
    }

    @Test
    @DisplayName("내 초대 코드 조회 - 방 없음 실패")
    void getMyInviteCode_Fail_RoomNotFound() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(999L, 1L))
                .willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> inviteController.getMyInviteCode(999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);

        verify(inviteService, times(1)).getOrCreateMyInviteCode(999L, 1L);
    }

    // ====================== 초대 코드 정보 확인 테스트 ======================

    @Test
    @DisplayName("초대 코드 만료 시간 정보 포함")
    void inviteCodeResponse_ExpiresAt() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getBody()).isNotNull();
        InviteCodeResponse data = response.getBody().getData();
        
        assertThat(data.getExpiresAt()).isNotNull();
        assertThat(data.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(2));
        assertThat(data.getExpiresAt()).isBefore(LocalDateTime.now().plusHours(4));
    }

    @Test
    @DisplayName("초대 코드 활성 상태 확인")
    void inviteCodeResponse_IsActive() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getBody()).isNotNull();
        InviteCodeResponse data = response.getBody().getData();
        
        assertThat(data.isActive()).isTrue();
        assertThat(data.isValid()).isTrue();
    }

    @Test
    @DisplayName("만료된 초대 코드 응답 확인")
    void inviteCodeResponse_Expired() {
        // given
        RoomInviteCode expiredCode = RoomInviteCode.builder()
                .inviteCode("EXPIRED1")
                .room(testRoom)
                .createdBy(testUser)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(expiredCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getBody()).isNotNull();
        InviteCodeResponse data = response.getBody().getData();
        
        assertThat(data.isValid()).isFalse(); // 만료됨
        assertThat(data.getExpiresAt()).isBefore(LocalDateTime.now());
    }

    // ====================== 엣지 케이스 테스트 ======================

    @Test
    @DisplayName("다른 사용자가 같은 방의 초대 코드 조회")
    void getMyInviteCode_DifferentUser_SameRoom() {
        // given - User1의 코드
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // given - User2의 코드
        RoomInviteCode user2Code = RoomInviteCode.create("XYZ98765", testRoom, testUser2);
        given(inviteService.getOrCreateMyInviteCode(1L, 2L)).willReturn(user2Code);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response1 = inviteController.getMyInviteCode(1L);
        
        given(currentUser.getUserId()).willReturn(2L); // 사용자 변경
        ResponseEntity<RsData<InviteCodeResponse>> response2 = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response1.getBody().getData().getInviteCode()).isEqualTo("ABC12345");
        assertThat(response2.getBody().getData().getInviteCode()).isEqualTo("XYZ98765");
        assertThat(response1.getBody().getData().getRoomId())
                .isEqualTo(response2.getBody().getData().getRoomId()); // 같은 방
    }

    @Test
    @DisplayName("초대 코드 재조회 시 같은 코드 반환")
    void getMyInviteCode_MultipleRequests_SameCode() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response1 = inviteController.getMyInviteCode(1L);
        ResponseEntity<RsData<InviteCodeResponse>> response2 = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response1.getBody().getData().getInviteCode())
                .isEqualTo(response2.getBody().getData().getInviteCode());
        assertThat(response1.getBody().getData().getInviteCode()).isEqualTo("ABC12345");

        verify(inviteService, times(2)).getOrCreateMyInviteCode(1L, 1L);
    }

    @Test
    @DisplayName("응답 메시지 형식 확인")
    void response_MessageFormat() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isNotBlank();
        assertThat(response.getBody().getMessage()).contains("초대 코드");
    }

    @Test
    @DisplayName("HTTP 상태 코드 확인")
    void response_HttpStatus() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("방 정보 포함 확인")
    void inviteCodeResponse_RoomInfo() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getBody()).isNotNull();
        InviteCodeResponse data = response.getBody().getData();
        
        assertThat(data.getRoomId()).isEqualTo(1L);
        assertThat(data.getRoomTitle()).isEqualTo("테스트 방");
    }

    @Test
    @DisplayName("생성자 정보 포함 확인")
    void inviteCodeResponse_CreatorInfo() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(inviteService.getOrCreateMyInviteCode(1L, 1L)).willReturn(testInviteCode);

        // when
        ResponseEntity<RsData<InviteCodeResponse>> response = inviteController.getMyInviteCode(1L);

        // then
        assertThat(response.getBody()).isNotNull();
        InviteCodeResponse data = response.getBody().getData();
        
        assertThat(data.getCreatedByNickname()).isEqualTo("테스트유저");
    }
}
