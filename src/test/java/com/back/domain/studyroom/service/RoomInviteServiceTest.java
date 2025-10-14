package com.back.domain.studyroom.service;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomInviteCode;
import com.back.domain.studyroom.repository.RoomInviteCodeRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.common.enums.Role;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomInviteService 테스트")
class RoomInviteServiceTest {

    @Mock
    private RoomInviteCodeRepository inviteCodeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RoomInviteService inviteService;

    private User testUser;
    private User testUser2;
    private Room testRoom;
    private Room privateRoom;
    private RoomInviteCode testInviteCode;

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

        // Room에 ID 설정 (리플렉션)
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

        // 테스트 초대 코드 생성
        testInviteCode = RoomInviteCode.create("ABC12345", testRoom, testUser);
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

    // ====================== 초대 코드 생성 테스트 ======================

    @Test
    @DisplayName("초대 코드 생성 - 성공 (신규)")
    void createInviteCode_Success_New() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 1L))
                .willReturn(Optional.empty());
        given(inviteCodeRepository.existsByInviteCode(anyString())).willReturn(false);
        given(inviteCodeRepository.save(any(RoomInviteCode.class))).willAnswer(i -> i.getArgument(0));

        // when
        RoomInviteCode result = inviteService.getOrCreateMyInviteCode(1L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getInviteCode()).hasSize(8);
        assertThat(result.getRoom()).isEqualTo(testRoom);
        assertThat(result.getCreatedBy()).isEqualTo(testUser);
        assertThat(result.isValid()).isTrue();
        assertThat(result.isActive()).isTrue();

        verify(inviteCodeRepository, times(1)).save(any(RoomInviteCode.class));
    }

    @Test
    @DisplayName("초대 코드 조회 - 기존 활성 코드 반환")
    void getInviteCode_Success_Existing() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 1L))
                .willReturn(Optional.of(testInviteCode));

        // when
        RoomInviteCode result = inviteService.getOrCreateMyInviteCode(1L, 1L);

        // then
        assertThat(result).isEqualTo(testInviteCode);
        assertThat(result.getInviteCode()).isEqualTo("ABC12345");
        assertThat(result.isValid()).isTrue();

        verify(inviteCodeRepository, never()).save(any(RoomInviteCode.class));
    }

    @Test
    @DisplayName("초대 코드 생성 - 만료된 코드 있으면 새로 생성")
    void createInviteCode_Success_Expired() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        // 만료된 초대 코드 생성
        RoomInviteCode expiredCode = RoomInviteCode.builder()
                .inviteCode("EXPIRED1")
                .room(testRoom)
                .createdBy(testUser)
                .expiresAt(LocalDateTime.now().minusHours(1)) // 1시간 전 만료
                .build();

        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 1L))
                .willReturn(Optional.of(expiredCode));
        given(inviteCodeRepository.existsByInviteCode(anyString())).willReturn(false);
        given(inviteCodeRepository.save(any(RoomInviteCode.class))).willAnswer(i -> i.getArgument(0));

        // when
        RoomInviteCode result = inviteService.getOrCreateMyInviteCode(1L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getInviteCode()).isNotEqualTo("EXPIRED1");
        assertThat(result.isValid()).isTrue();
        assertThat(expiredCode.isActive()).isFalse(); // 기존 코드 비활성화

        verify(inviteCodeRepository, times(1)).save(any(RoomInviteCode.class));
    }

    @Test
    @DisplayName("초대 코드 생성 - 비공개 방도 생성 가능")
    void createInviteCode_Success_PrivateRoom() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(roomRepository.findById(2L)).willReturn(Optional.of(privateRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(2L, 1L))
                .willReturn(Optional.empty());
        given(inviteCodeRepository.existsByInviteCode(anyString())).willReturn(false);
        given(inviteCodeRepository.save(any(RoomInviteCode.class))).willAnswer(i -> i.getArgument(0));

        // when
        RoomInviteCode result = inviteService.getOrCreateMyInviteCode(2L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRoom()).isEqualTo(privateRoom);
        assertThat(result.isValid()).isTrue();

        verify(inviteCodeRepository, times(1)).save(any(RoomInviteCode.class));
    }

    @Test
    @DisplayName("초대 코드 생성 - 방 없음 실패")
    void createInviteCode_Fail_RoomNotFound() {
        // given
        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inviteService.getOrCreateMyInviteCode(999L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);

        verify(roomRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("초대 코드 생성 - 사용자 없음 실패")
    void createInviteCode_Fail_UserNotFound() {
        // given
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inviteService.getOrCreateMyInviteCode(1L, 999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(roomRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(999L);
    }

    // ====================== 초대 코드로 방 조회 테스트 ======================

    @Test
    @DisplayName("초대 코드로 방 조회 - 성공 (Redis 없음 → DB 조회)")
    void getRoomByInviteCode_Success_FromDB() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null); // Redis에 없음
        given(inviteCodeRepository.findByInviteCode("ABC12345"))
                .willReturn(Optional.of(testInviteCode));

        // when
        Room result = inviteService.getRoomByInviteCode("ABC12345");

        // then
        assertThat(result).isEqualTo(testRoom);
        assertThat(result.getId()).isEqualTo(1L);

        verify(inviteCodeRepository, times(1)).findByInviteCode("ABC12345");
    }

    @Test
    @DisplayName("초대 코드로 방 조회 - 성공 (Redis 있음)")
    void getRoomByInviteCode_Success_FromRedis() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("invite:code:ABC12345")).willReturn("1"); // Redis에 있음
        given(inviteCodeRepository.findByInviteCode("ABC12345"))
                .willReturn(Optional.of(testInviteCode));

        // when
        Room result = inviteService.getRoomByInviteCode("ABC12345");

        // then
        assertThat(result).isEqualTo(testRoom);
        assertThat(result.getId()).isEqualTo(1L);

        verify(inviteCodeRepository, times(1)).findByInviteCode("ABC12345");
    }

    @Test
    @DisplayName("초대 코드로 방 조회 - 잘못된 코드")
    void getRoomByInviteCode_Fail_InvalidCode() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);
        given(inviteCodeRepository.findByInviteCode("INVALID1"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inviteService.getRoomByInviteCode("INVALID1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INVITE_CODE);

        verify(inviteCodeRepository, times(1)).findByInviteCode("INVALID1");
    }

    @Test
    @DisplayName("초대 코드로 방 조회 - 만료된 코드")
    void getRoomByInviteCode_Fail_Expired() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        RoomInviteCode expiredCode = RoomInviteCode.builder()
                .inviteCode("EXPIRED1")
                .room(testRoom)
                .createdBy(testUser)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        given(valueOperations.get(anyString())).willReturn(null);
        given(inviteCodeRepository.findByInviteCode("EXPIRED1"))
                .willReturn(Optional.of(expiredCode));

        // when & then
        assertThatThrownBy(() -> inviteService.getRoomByInviteCode("EXPIRED1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVITE_CODE_EXPIRED);

        verify(inviteCodeRepository, times(1)).findByInviteCode("EXPIRED1");
    }

    @Test
    @DisplayName("초대 코드로 방 조회 - 비활성화된 코드")
    void getRoomByInviteCode_Fail_Inactive() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        RoomInviteCode inactiveCode = RoomInviteCode.create("INACTIVE1", testRoom, testUser);
        inactiveCode.deactivate();

        given(valueOperations.get(anyString())).willReturn(null);
        given(inviteCodeRepository.findByInviteCode("INACTIVE1"))
                .willReturn(Optional.of(inactiveCode));

        // when & then
        assertThatThrownBy(() -> inviteService.getRoomByInviteCode("INACTIVE1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVITE_CODE_EXPIRED);

        verify(inviteCodeRepository, times(1)).findByInviteCode("INACTIVE1");
    }

    // ====================== 초대 코드 유효성 검증 테스트 ======================

    @Test
    @DisplayName("초대 코드 유효성 검증 - 유효함")
    void inviteCode_IsValid_True() {
        // given
        RoomInviteCode validCode = RoomInviteCode.create("VALID123", testRoom, testUser);

        // when
        boolean isValid = validCode.isValid();

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("초대 코드 유효성 검증 - 만료됨")
    void inviteCode_IsValid_False_Expired() {
        // given
        RoomInviteCode expiredCode = RoomInviteCode.builder()
                .inviteCode("EXPIRED1")
                .room(testRoom)
                .createdBy(testUser)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        // when
        boolean isValid = expiredCode.isValid();

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("초대 코드 유효성 검증 - 비활성화됨")
    void inviteCode_IsValid_False_Inactive() {
        // given
        RoomInviteCode inactiveCode = RoomInviteCode.create("INACTIVE1", testRoom, testUser);
        inactiveCode.deactivate();

        // when
        boolean isValid = inactiveCode.isValid();

        // then
        assertThat(isValid).isFalse();
        assertThat(inactiveCode.isActive()).isFalse();
    }

    @Test
    @DisplayName("초대 코드 만료 시간 확인 - 3시간")
    void inviteCode_ExpiresIn3Hours() {
        // given
        LocalDateTime now = LocalDateTime.now();
        RoomInviteCode code = RoomInviteCode.create("TEST1234", testRoom, testUser);

        // when
        LocalDateTime expiresAt = code.getExpiresAt();

        // then
        assertThat(expiresAt).isAfter(now.plusHours(2).plusMinutes(59));
        assertThat(expiresAt).isBefore(now.plusHours(3).plusMinutes(1));
    }

    // ====================== 초대 코드 형식 검증 테스트 ======================

    @Test
    @DisplayName("초대 코드 형식 - 8자리 생성")
    void inviteCode_Format_8Characters() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 1L))
                .willReturn(Optional.empty());
        given(inviteCodeRepository.existsByInviteCode(anyString())).willReturn(false);
        given(inviteCodeRepository.save(any(RoomInviteCode.class))).willAnswer(i -> i.getArgument(0));

        // when
        RoomInviteCode result = inviteService.getOrCreateMyInviteCode(1L, 1L);

        // then
        assertThat(result.getInviteCode()).hasSize(8);
        assertThat(result.getInviteCode()).matches("^[A-Z2-9]{8}$"); // 대문자 + 숫자만
    }

    @Test
    @DisplayName("초대 코드 형식 - 혼동 문자 제외 확인")
    void inviteCode_Format_NoConfusingChars() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 1L))
                .willReturn(Optional.empty());
        given(inviteCodeRepository.existsByInviteCode(anyString())).willReturn(false);
        given(inviteCodeRepository.save(any(RoomInviteCode.class))).willAnswer(i -> i.getArgument(0));

        // when
        RoomInviteCode result = inviteService.getOrCreateMyInviteCode(1L, 1L);

        // then
        String code = result.getInviteCode();
        assertThat(code).doesNotContain("0", "O", "1", "I", "l"); // 혼동 문자 없음
    }

    // ====================== 엣지 케이스 테스트 ======================

    @Test
    @DisplayName("다중 사용자가 같은 방에 대한 초대 코드 생성")
    void multipleUsers_CreateInviteCode_SameRoom() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        
        // given - User1의 코드
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 1L))
                .willReturn(Optional.empty());
        given(inviteCodeRepository.existsByInviteCode(anyString())).willReturn(false);
        given(inviteCodeRepository.save(any(RoomInviteCode.class))).willAnswer(i -> i.getArgument(0));

        // given - User2의 코드
        given(userRepository.findById(2L)).willReturn(Optional.of(testUser2));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 2L))
                .willReturn(Optional.empty());

        // when
        RoomInviteCode code1 = inviteService.getOrCreateMyInviteCode(1L, 1L);
        RoomInviteCode code2 = inviteService.getOrCreateMyInviteCode(1L, 2L);

        // then
        assertThat(code1.getInviteCode()).isNotEqualTo(code2.getInviteCode()); // 다른 코드
        assertThat(code1.getCreatedBy()).isEqualTo(testUser);
        assertThat(code2.getCreatedBy()).isEqualTo(testUser2);
        assertThat(code1.getRoom()).isEqualTo(code2.getRoom()); // 같은 방

        verify(inviteCodeRepository, times(2)).save(any(RoomInviteCode.class));
    }

    @Test
    @DisplayName("초대 코드 재생성 제한 - 활성 코드 있으면 새로 생성 안 됨")
    void createInviteCode_Limit_ExistingActiveCode() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 1L))
                .willReturn(Optional.of(testInviteCode));

        // when
        RoomInviteCode result1 = inviteService.getOrCreateMyInviteCode(1L, 1L);
        RoomInviteCode result2 = inviteService.getOrCreateMyInviteCode(1L, 1L);

        // then
        assertThat(result1).isSameAs(result2); // 같은 인스턴스
        assertThat(result1.getInviteCode()).isEqualTo("ABC12345");

        verify(inviteCodeRepository, never()).save(any(RoomInviteCode.class)); // 저장 안 됨
    }

    @Test
    @DisplayName("초대 코드 비활성화 후 재생성")
    void createInviteCode_AfterDeactivation() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        RoomInviteCode deactivatedCode = RoomInviteCode.create("OLD12345", testRoom, testUser);
        deactivatedCode.deactivate();

        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inviteCodeRepository.findByRoomIdAndCreatedByIdAndIsActiveTrue(1L, 1L))
                .willReturn(Optional.of(deactivatedCode)); // 비활성 코드 반환
        given(inviteCodeRepository.existsByInviteCode(anyString())).willReturn(false);
        given(inviteCodeRepository.save(any(RoomInviteCode.class))).willAnswer(i -> i.getArgument(0));

        // when
        RoomInviteCode result = inviteService.getOrCreateMyInviteCode(1L, 1L);

        // then
        assertThat(result.getInviteCode()).isNotEqualTo("OLD12345"); // 새 코드
        assertThat(result.isValid()).isTrue();
        assertThat(deactivatedCode.isActive()).isFalse(); // 기존 코드는 비활성 유지

        verify(inviteCodeRepository, times(1)).save(any(RoomInviteCode.class));
    }
}
