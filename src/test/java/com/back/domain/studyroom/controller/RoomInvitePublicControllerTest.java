package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.JoinRoomResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomInviteCode;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import com.back.domain.studyroom.service.RoomInviteService;
import com.back.domain.studyroom.service.RoomService;
import com.back.domain.user.common.enums.Role;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomInvitePublicController 테스트")
class RoomInvitePublicControllerTest {

    @Mock
    private RoomInviteService inviteService;

    @Mock
    private RoomService roomService;

    @Mock
    private CurrentUser currentUser;
    
    @Mock
    private com.back.domain.studyroom.service.AvatarService avatarService;

    @InjectMocks
    private RoomInvitePublicController invitePublicController;

    private User testUser;
    private User testUser2;
    private Room testRoom;
    private Room privateRoom;
    private RoomInviteCode testInviteCode;
    private RoomInviteCode privateRoomInviteCode;
    private RoomMember testMember;
    private RoomMember privateMember;

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

        // 테스트 멤버 생성
        testMember = RoomMember.createVisitor(testRoom, testUser);
        privateMember = RoomMember.createVisitor(privateRoom, testUser);
        
        // AvatarService Mock 기본 설정 - 랜덤 아바타 ID 반환
        // Lenient: 일부 테스트에서만 사용되므로 불필요한 Stubbing 경고 무시
        lenient().when(avatarService.assignRandomAvatar()).thenReturn(2L);
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

    // ====================== 초대 코드로 입장 테스트 ======================

    @Test
    @DisplayName("초대 코드로 입장 - 성공 (공개 방)")
    void joinByInviteCode_Success_PublicRoom() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("초대 코드");

        verify(currentUser, times(1)).getUserId();
        verify(inviteService, times(1)).getRoomByInviteCode("ABC12345");
        verify(roomService, times(1)).joinRoom(eq(1L), isNull(), eq(2L), eq(false));
    }

    @Test
    @DisplayName("초대 코드로 입장 - 성공 (비공개 방, 비밀번호 무시)")
    void joinByInviteCode_Success_PrivateRoom_PasswordIgnored() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("PRIVATE1")).willReturn(privateRoom);
        given(roomService.joinRoom(eq(2L), isNull(), eq(2L), eq(false))).willReturn(privateMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("PRIVATE1");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        // 비밀번호 null로 전달되는지 확인 (비밀번호 무시), registerOnline=false
        verify(roomService, times(1)).joinRoom(eq(2L), isNull(), eq(2L), eq(false));
    }

    @Test
    @DisplayName("초대 코드로 입장 - 응답에 방 정보 포함")
    void joinByInviteCode_ResponseContainsRoomInfo() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        
        JoinRoomResponse data = response.getBody().getData();
        assertThat(data.getRoomId()).isEqualTo(1L);
        assertThat(data.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("초대 코드로 입장 - 응답에 사용자 정보 포함")
    void joinByInviteCode_ResponseContainsUserInfo() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response.getBody()).isNotNull();
        JoinRoomResponse data = response.getBody().getData();
        
        assertThat(data.getUserId()).isEqualTo(1L);
        assertThat(data.getRole()).isEqualTo(RoomRole.VISITOR);
    }

    @Test
    @DisplayName("초대 코드로 입장 - 잘못된 코드 실패")
    void joinByInviteCode_Fail_InvalidCode() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("INVALID1"))
                .willThrow(new CustomException(ErrorCode.INVALID_INVITE_CODE));

        // when & then
        assertThatThrownBy(() -> invitePublicController.joinByInviteCode("INVALID1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INVITE_CODE);

        verify(inviteService, times(1)).getRoomByInviteCode("INVALID1");
        verify(roomService, never()).joinRoom(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("초대 코드로 입장 - 만료된 코드 실패")
    void joinByInviteCode_Fail_ExpiredCode() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("EXPIRED1"))
                .willThrow(new CustomException(ErrorCode.INVITE_CODE_EXPIRED));

        // when & then
        assertThatThrownBy(() -> invitePublicController.joinByInviteCode("EXPIRED1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVITE_CODE_EXPIRED);

        verify(inviteService, times(1)).getRoomByInviteCode("EXPIRED1");
        verify(roomService, never()).joinRoom(anyLong(), any(), anyLong());
    }

    // ====================== 비밀번호 처리 테스트 ======================

    @Test
    @DisplayName("초대 코드 입장 시 비밀번호 파라미터가 항상 null")
    void joinByInviteCode_PasswordAlwaysNull() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        invitePublicController.joinByInviteCode("ABC12345");

        // then
        // 비밀번호가 항상 null로 전달되는지 확인, registerOnline=false
        verify(roomService, times(1)).joinRoom(eq(1L), isNull(), eq(2L), eq(false));
    }

    @Test
    @DisplayName("비밀번호가 설정된 방도 초대 코드로 입장 가능")
    void joinByInviteCode_PrivateRoomAccessible() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("PRIVATE1")).willReturn(privateRoom);
        given(roomService.joinRoom(eq(2L), isNull(), eq(2L), eq(false))).willReturn(privateMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("PRIVATE1");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        // 비공개 방인데도 비밀번호 없이 입장 성공, registerOnline=false
        verify(roomService, times(1)).joinRoom(eq(2L), isNull(), eq(2L), eq(false));
    }

    // ====================== HTTP 응답 테스트 ======================

    @Test
    @DisplayName("HTTP 상태 코드 확인")
    void joinByInviteCode_HttpStatus() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("응답 메시지 형식 확인")
    void joinByInviteCode_ResponseMessage() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isNotBlank();
        assertThat(response.getBody().getMessage()).contains("초대 코드");
    }

    // ====================== 엣지 케이스 테스트 ======================

    @Test
    @DisplayName("같은 사용자가 같은 초대 코드로 여러 번 입장 시도")
    void joinByInviteCode_MultipleAttempts_SameUser() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response1 = 
                invitePublicController.joinByInviteCode("ABC12345");
        ResponseEntity<RsData<JoinRoomResponse>> response2 = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(inviteService, times(2)).getRoomByInviteCode("ABC12345");
        verify(roomService, times(2)).joinRoom(eq(1L), isNull(), eq(2L), eq(false));
    }

    @Test
    @DisplayName("다른 사용자가 같은 초대 코드로 입장")
    void joinByInviteCode_DifferentUsers_SameCode() {
        // given - User2
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        
        RoomMember member2 = RoomMember.createVisitor(testRoom, testUser2);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(member2);

        // when - User2 입장
        ResponseEntity<RsData<JoinRoomResponse>> response1 = 
                invitePublicController.joinByInviteCode("ABC12345");

        // given - User1 (코드 생성자도 입장 가능)
        given(currentUser.getUserId()).willReturn(1L);
        given(roomService.joinRoom(eq(1L), isNull(), eq(1L), eq(false))).willReturn(testMember);

        // when - User1 입장
        ResponseEntity<RsData<JoinRoomResponse>> response2 = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(inviteService, times(2)).getRoomByInviteCode("ABC12345");
        verify(roomService, times(1)).joinRoom(eq(1L), isNull(), eq(2L), eq(false));
        verify(roomService, times(1)).joinRoom(eq(1L), isNull(), eq(1L), eq(false));
    }

    @Test
    @DisplayName("초대 코드 입장 시 방 최대 인원 초과")
    void joinByInviteCode_Fail_RoomFull() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false)))
                .willThrow(new CustomException(ErrorCode.ROOM_FULL));

        // when & then
        assertThatThrownBy(() -> invitePublicController.joinByInviteCode("ABC12345"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_FULL);

        verify(inviteService, times(1)).getRoomByInviteCode("ABC12345");
        verify(roomService, times(1)).joinRoom(eq(1L), isNull(), eq(2L), eq(false));
    }

    @Test
    @DisplayName("초대 코드 입장 시 이미 참여 중인 방")
    void joinByInviteCode_Fail_AlreadyJoined() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false)))
                .willThrow(new CustomException(ErrorCode.ALREADY_JOINED_ROOM));

        // when & then
        assertThatThrownBy(() -> invitePublicController.joinByInviteCode("ABC12345"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_JOINED_ROOM);

        verify(inviteService, times(1)).getRoomByInviteCode("ABC12345");
        verify(roomService, times(1)).joinRoom(eq(1L), isNull(), eq(2L), eq(false));
    }

    @Test
    @DisplayName("응답 데이터 완전성 검증")
    void joinByInviteCode_ResponseDataCompleteness() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response.getBody()).isNotNull();
        JoinRoomResponse data = response.getBody().getData();
        
        // JoinRoomResponse 실제 필드에 맞춰 검증
        assertThat(data.getRoomId()).isNotNull();
        assertThat(data.getUserId()).isNotNull();
        assertThat(data.getRole()).isNotNull();
        assertThat(data.getJoinedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대 코드 대소문자 구분 확인")
    void joinByInviteCode_CaseSensitive() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 정확한 코드가 서비스로 전달되는지 확인
        verify(inviteService, times(1)).getRoomByInviteCode("ABC12345");
    }

    @Test
    @DisplayName("VISITOR 권한으로 입장 확인")
    void joinByInviteCode_RoleIsVisitor() {
        // given
        given(currentUser.getUserId()).willReturn(2L);
        given(inviteService.getRoomByInviteCode("ABC12345")).willReturn(testRoom);
        given(roomService.joinRoom(eq(1L), isNull(), eq(2L), eq(false))).willReturn(testMember);

        // when
        ResponseEntity<RsData<JoinRoomResponse>> response = 
                invitePublicController.joinByInviteCode("ABC12345");

        // then
        assertThat(response.getBody()).isNotNull();
        JoinRoomResponse data = response.getBody().getData();
        
        // 초대 코드로 입장하면 VISITOR 권한
        assertThat(data.getRole()).isEqualTo(RoomRole.VISITOR);
    }
}
