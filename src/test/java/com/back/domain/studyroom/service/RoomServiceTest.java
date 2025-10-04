package com.back.domain.studyroom.service;

import com.back.domain.studyroom.config.StudyRoomProperties;
import com.back.domain.studyroom.entity.*;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
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
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService 테스트")
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyRoomProperties properties;

    @Mock
    private RoomParticipantService roomParticipantService;

    @InjectMocks
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
    }

    @Test
    @DisplayName("방 생성 - 성공")
    void createRoom_Success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(roomRepository.save(any(Room.class))).willReturn(testRoom);
        given(roomMemberRepository.save(any(RoomMember.class))).willReturn(testMember);

        // when
        Room createdRoom = roomService.createRoom(
                "테스트 방",
                "테스트 설명",
                false,
                null,
                10,
                1L,
                true  // useWebRTC
        );

        // then
        assertThat(createdRoom).isNotNull();
        assertThat(createdRoom.getTitle()).isEqualTo("테스트 방");
        assertThat(createdRoom.getDescription()).isEqualTo("테스트 설명");
        verify(roomRepository, times(1)).save(any(Room.class));
        verify(roomMemberRepository, times(1)).save(any(RoomMember.class));
    }

    @Test
    @DisplayName("방 생성 - 사용자 없음 실패")
    void createRoom_UserNotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomService.createRoom(
                "테스트 방",
                "테스트 설명",
                false,
                null,
                10,
                999L,
                true  // useWebRTC
        ))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("방 입장 - 성공")
    void joinRoom_Success() {
        // given
        given(roomRepository.findByIdWithLock(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(2L)).willReturn(Optional.of(testUser));
        given(roomMemberRepository.findByRoomIdAndUserId(1L, 2L)).willReturn(Optional.empty());
        given(roomParticipantService.getParticipantCount(1L)).willReturn(0L); // Redis 카운트

        // when
        RoomMember joinedMember = roomService.joinRoom(1L, null, 2L);

        // then
        assertThat(joinedMember).isNotNull();
        assertThat(joinedMember.getRole()).isEqualTo(RoomRole.VISITOR);
        verify(roomParticipantService, times(1)).enterRoom(2L, 1L); // Redis 입장 확인
        verify(roomMemberRepository, never()).save(any(RoomMember.class)); // DB 저장 안됨!
    }

    @Test
    @DisplayName("방 입장 - 방 없음 실패")
    void joinRoom_RoomNotFound() {
        // given
        given(roomRepository.findByIdWithLock(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomService.joinRoom(999L, null, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("방 입장 - 비밀번호 틀림")
    void joinRoom_WrongPassword() {
        // given
        Room privateRoom = Room.create(
                "비공개 방",
                "설명",
                true,
                "1234",
                10,
                testUser,
                null,
                true  // useWebRTC
        );
        given(roomRepository.findByIdWithLock(1L)).willReturn(Optional.of(privateRoom));
        given(roomParticipantService.getParticipantCount(1L)).willReturn(0L); // Redis 카운트

        // when & then
        assertThatThrownBy(() -> roomService.joinRoom(1L, "wrong", 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_PASSWORD_INCORRECT);
    }

    @Test
    @DisplayName("방 나가기 - 성공")
    void leaveRoom_Success() {
        // given
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

        // when
        roomService.leaveRoom(1L, 1L);

        // then
        verify(roomParticipantService, times(1)).exitRoom(1L, 1L); // Redis 퇴장 확인
    }

    @Test
    @DisplayName("입장 가능한 공개 방 목록 조회")
    void getJoinableRooms_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Room> rooms = Arrays.asList(testRoom);
        Page<Room> roomPage = new PageImpl<>(rooms, pageable, 1);
        
        given(roomRepository.findJoinablePublicRooms(pageable)).willReturn(roomPage);

        // when
        Page<Room> result = roomService.getJoinableRooms(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 방");
        verify(roomRepository, times(1)).findJoinablePublicRooms(pageable);
    }

    @Test
    @DisplayName("방 상세 정보 조회 - 성공")
    void getRoomDetail_Success() {
        // given
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        // 공개방이므로 existsByRoomIdAndUserId는 호출되지 않음 - stub 제거

        // when
        Room result = roomService.getRoomDetail(1L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("테스트 방");
    }

    @Test
    @DisplayName("방 상세 정보 조회 - 비공개 방 권한 없음")
    void getRoomDetail_PrivateRoomForbidden() {
        // given
        Room privateRoom = Room.create(
                "비공개 방",
                "설명",
                true,
                "1234",
                10,
                testUser,
                null,
                true  // useWebRTC
        );
        given(roomRepository.findById(1L)).willReturn(Optional.of(privateRoom));
        given(roomMemberRepository.existsByRoomIdAndUserId(1L, 2L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> roomService.getRoomDetail(1L, 2L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_FORBIDDEN);
    }

    @Test
    @DisplayName("방 설정 변경 - 성공")
    void updateRoomSettings_Success() {
        // given
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

        // when
        roomService.updateRoomSettings(
                1L,
                "변경된 제목",
                "변경된 설명",
                15,
                true,
                true,
                false,
                1L
        );

        // then
        assertThat(testRoom.getTitle()).isEqualTo("변경된 제목");
        assertThat(testRoom.getDescription()).isEqualTo("변경된 설명");
        assertThat(testRoom.getMaxParticipants()).isEqualTo(15);
    }

    @Test
    @DisplayName("방 설정 변경 - 방장 권한 없음")
    void updateRoomSettings_NotOwner() {
        // given
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

        // when & then
        assertThatThrownBy(() -> roomService.updateRoomSettings(
                1L,
                "변경된 제목",
                "변경된 설명",
                15,
                true,
                true,
                false,
                999L // 다른 사용자
        ))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_MANAGER);
    }

    @Test
    @DisplayName("방 종료 - 성공")
    void terminateRoom_Success() {
        // given
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(roomParticipantService.getParticipants(1L)).willReturn(java.util.Set.of()); // 온라인 사용자 없음

        // when
        roomService.terminateRoom(1L, 1L);

        // then
        assertThat(testRoom.getStatus()).isEqualTo(RoomStatus.TERMINATED);
        assertThat(testRoom.isActive()).isFalse();
    }

    @Test
    @DisplayName("방 종료 - 방장 권한 없음")
    void terminateRoom_NotOwner() {
        // given
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

        // when & then
        assertThatThrownBy(() -> roomService.terminateRoom(1L, 999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_MANAGER);
    }

    @Test
    @DisplayName("인기 방 목록 조회")
    void getPopularRooms_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Room> rooms = Arrays.asList(testRoom);
        Page<Room> roomPage = new PageImpl<>(rooms, pageable, 1);
        
        given(roomRepository.findPopularRooms(pageable)).willReturn(roomPage);

        // when
        Page<Room> result = roomService.getPopularRooms(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(roomRepository, times(1)).findPopularRooms(pageable);
    }

    @Test
    @DisplayName("멤버 추방 - 성공")
    void kickMember_Success() {
        // given
        RoomMember hostMember = RoomMember.createHost(testRoom, testUser);
        
        User targetUser = User.builder()
                .id(2L)
                .username("target")
                .email("target@test.com")
                .role(Role.USER)
                .build();
        UserProfile targetProfile = new UserProfile();
        targetProfile.setNickname("대상유저");
        targetUser.setUserProfile(targetProfile);
        
        RoomMember targetMember = RoomMember.createVisitor(testRoom, targetUser);
        
        given(roomMemberRepository.findByRoomIdAndUserId(1L, 1L)).willReturn(Optional.of(hostMember));
        given(roomMemberRepository.findByRoomIdAndUserId(1L, 2L)).willReturn(Optional.of(targetMember));

        // when
        roomService.kickMember(1L, 2L, 1L);

        // then
        verify(roomParticipantService, times(1)).exitRoom(2L, 1L); // Redis 퇴장 확인
    }

    @Test
    @DisplayName("멤버 추방 - 권한 없음")
    void kickMember_NoPermission() {
        // given
        RoomMember visitorMember = RoomMember.createVisitor(testRoom, testUser);
        
        given(roomMemberRepository.findByRoomIdAndUserId(1L, 1L)).willReturn(Optional.of(visitorMember));

        // when & then
        assertThatThrownBy(() -> roomService.kickMember(1L, 2L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_MANAGER);
    }

    @Test
    @DisplayName("방 생성 - WebRTC 활성화")
    void createRoom_WithWebRTC() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(roomMemberRepository.save(any(RoomMember.class))).willReturn(testMember);

        // when
        Room createdRoom = roomService.createRoom(
                "WebRTC 방",
                "화상 채팅 가능",
                false,
                null,
                10,
                1L,
                true  // WebRTC 사용
        );

        // then
        assertThat(createdRoom).isNotNull();
        assertThat(createdRoom.isAllowCamera()).isTrue();
        assertThat(createdRoom.isAllowAudio()).isTrue();
        assertThat(createdRoom.isAllowScreenShare()).isTrue();
    }

    @Test
    @DisplayName("방 생성 - WebRTC 비활성화")
    void createRoom_WithoutWebRTC() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(roomMemberRepository.save(any(RoomMember.class))).willReturn(testMember);

        // when
        Room createdRoom = roomService.createRoom(
                "채팅 전용 방",
                "텍스트만 가능",
                false,
                null,
                50,  // WebRTC 없으면 더 많은 인원 가능
                1L,
                false  // WebRTC 미사용
        );

        // then
        assertThat(createdRoom).isNotNull();
        assertThat(createdRoom.isAllowCamera()).isFalse();
        assertThat(createdRoom.isAllowAudio()).isFalse();
        assertThat(createdRoom.isAllowScreenShare()).isFalse();
    }
}
