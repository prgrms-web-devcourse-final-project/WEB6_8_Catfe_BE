package com.back.domain.chat.room.service;

import com.back.domain.chat.room.dto.RoomChatMessageDto;
import com.back.domain.chat.room.dto.RoomChatPageResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.studyroom.repository.RoomChatMessageRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
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
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("RoomChatService 테스트")
class RoomChatServiceTest {

    @Mock
    private RoomChatMessageRepository roomChatMessageRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoomChatService roomChatService;

    private Room testRoom;
    private User testUser;
    private UserProfile testUserProfile;
    private RoomChatMessage testMessage;

    @BeforeEach
    void setUp() throws Exception {
        // UserProfile 생성
        testUserProfile = createUserProfile("테스터", "https://example.com/profile.jpg");

        // User 생성 및 userProfiles 필드 설정
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("테스터")
                .build();

        // 리플렉션으로 userProfiles 필드 설정
        setUserProfile(testUser, testUserProfile);

        testRoom = Room.builder()
                .id(1L)
                .title("테스트 방")
                .description("테스트용 스터디룸")
                .build();

        testMessage = RoomChatMessage.builder()
                .id(1L)
                .room(testRoom)
                .user(testUser)
                .content("테스트 메시지")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // UserProfile 생성 헬퍼 메소드
    private UserProfile createUserProfile(String nickname, String profileImageUrl) throws Exception {
        UserProfile userProfile = new UserProfile();

        // 리플렉션으로 private 필드 설정
        setField(userProfile, "nickname", nickname);
        setField(userProfile, "profileImageUrl", profileImageUrl);
        setField(userProfile, "user", testUser);

        return userProfile;
    }

    // User의 userProfiles 필드 설정
    private void setUserProfile(User user, UserProfile profile) throws Exception {
        Field userProfilesField = User.class.getDeclaredField("userProfile");
        userProfilesField.setAccessible(true);
        userProfilesField.set(user, profile);
    }

    // 리플렉션으로 필드 값 설정하는 헬퍼 메소드
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("채팅 메시지 저장 성공")
    void t1() {
        // Given
        // createRequest 사용 후 필드 업데이트
        RoomChatMessageDto roomChatMessageDto = RoomChatMessageDto
                .createRequest("안녕하세요!", "TEXT")
                .withRoomId(1L)
                .withUserId(1L);

        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(roomChatMessageRepository.save(any(RoomChatMessage.class))).willReturn(testMessage);

        // When
        RoomChatMessage result = roomChatService.saveRoomChatMessage(roomChatMessageDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("테스트 메시지");

        verify(roomRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(roomChatMessageRepository).save(any(RoomChatMessage.class));
    }

    @Test
    @DisplayName("채팅 메시지 저장 실패 - 존재하지 않는 방")
    void t2() {
        RoomChatMessageDto roomChatMessageDto = RoomChatMessageDto
                .createRequest("메시지", "TEXT")
                .withRoomId(999L)
                .withUserId(1L);

        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomChatService.saveRoomChatMessage(roomChatMessageDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);

        verify(roomRepository).findById(999L);
        verify(userRepository, never()).findById(any());
        verify(roomChatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅 메시지 저장 실패 - 존재하지 않는 사용자")
    void t3() {
        RoomChatMessageDto roomChatMessageDto = RoomChatMessageDto
                .createRequest("메시지", "TEXT")
                .withRoomId(1L)
                .withUserId(999L);

        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomChatService.saveRoomChatMessage(roomChatMessageDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(roomRepository).findById(1L);
        verify(userRepository).findById(999L);
        verify(roomChatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅 기록 조회 성공 - before 파라미터 없음")
    void t4() {
        Long roomId = 1L;
        int page = 0;
        int size = 10;
        LocalDateTime before = null;

        List<RoomChatMessage> messages = Arrays.asList(testMessage);
        Page<RoomChatMessage> messagePage = new PageImpl<>(messages, PageRequest.of(page, size), 1);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
        given(roomChatMessageRepository.findMessagesByRoomId(eq(roomId), any(Pageable.class)))
                .willReturn(messagePage);

        RoomChatPageResponse result = roomChatService.getRoomChatHistory(roomId, page, size, before);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);

        RoomChatMessageDto messageDto = result.content().get(0);

        assertThat(messageDto.messageId()).isEqualTo(1L);
        assertThat(messageDto.roomId()).isEqualTo(1L);
        assertThat(messageDto.userId()).isEqualTo(1L);
        assertThat(messageDto.nickname()).isEqualTo("테스터");
        assertThat(messageDto.content()).isEqualTo("테스트 메시지");
        assertThat(messageDto.messageType()).isEqualTo("TEXT");

        verify(roomChatMessageRepository).findMessagesByRoomId(eq(roomId), any(Pageable.class));
        verify(roomChatMessageRepository, never()).findMessagesByRoomIdBefore(any(), any(), any());
    }

    @Test
    @DisplayName("채팅 기록 조회 성공 - before 파라미터 있음")
    void t5() {
        Long roomId = 1L;
        int page = 0;
        int size = 10;
        LocalDateTime before = LocalDateTime.now().minusHours(1);

        List<RoomChatMessage> messages = Arrays.asList(testMessage);
        Page<RoomChatMessage> messagePage = new PageImpl<>(messages, PageRequest.of(page, size), 1);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
        given(roomChatMessageRepository.findMessagesByRoomIdBefore(eq(roomId), eq(before), any(Pageable.class)))
                .willReturn(messagePage);

        RoomChatPageResponse result = roomChatService.getRoomChatHistory(roomId, page, size, before);

        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);

        RoomChatMessageDto messageDto = result.content().get(0);

        assertThat(messageDto.nickname()).isEqualTo("테스터");
        assertThat(messageDto.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");

        verify(roomChatMessageRepository).findMessagesByRoomIdBefore(eq(roomId), eq(before), any(Pageable.class));
        verify(roomChatMessageRepository, never()).findMessagesByRoomId(any(), any());
    }

    @Test
    @DisplayName("채팅 기록 조회 실패 - 존재하지 않는 방")
    void t6() {
        Long nonExistentRoomId = 999L;

        given(roomRepository.findById(nonExistentRoomId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomChatService.getRoomChatHistory(nonExistentRoomId, 0, 10, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);

        verify(roomRepository).findById(nonExistentRoomId);
        verify(roomChatMessageRepository, never()).findMessagesByRoomId(any(), any());
        verify(roomChatMessageRepository, never()).findMessagesByRoomIdBefore(any(), any(), any());
    }

    @Test
    @DisplayName("size 최대값 제한 테스트 - 100초과 요청")
    void t7() {
        Long roomId = 1L;
        int page = 0;
        int requestedSize = 150;
        LocalDateTime before = null;

        List<RoomChatMessage> messages = Arrays.asList(testMessage);
        Page<RoomChatMessage> messagePage = new PageImpl<>(messages, PageRequest.of(page, 100), 1);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
        given(roomChatMessageRepository.findMessagesByRoomId(eq(roomId), any(Pageable.class)))
                .willReturn(messagePage);

        RoomChatPageResponse result = roomChatService.getRoomChatHistory(roomId, page, requestedSize, before);

        assertThat(result).isNotNull();

        // size가 100으로 제한되었는지 확인
        verify(roomChatMessageRepository).findMessagesByRoomId(eq(roomId), argThat(pageable ->
                pageable.getPageSize() == 100
        ));
    }

    @Test
    @DisplayName("size 기본값 설정 테스트 - 0 요청")
    void t8() {
        Long roomId = 1L;
        int page = 0;
        int requestedSize = 0;
        LocalDateTime before = null;

        List<RoomChatMessage> messages = Arrays.asList(testMessage);
        Page<RoomChatMessage> messagePage = new PageImpl<>(messages, PageRequest.of(page, 20), 1);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
        given(roomChatMessageRepository.findMessagesByRoomId(eq(roomId), any(Pageable.class)))
                .willReturn(messagePage);

        RoomChatPageResponse result = roomChatService.getRoomChatHistory(roomId, page, requestedSize, before);

        assertThat(result).isNotNull();

        // size가 기본값 20으로 설정되었는지 확인
        verify(roomChatMessageRepository).findMessagesByRoomId(eq(roomId), argThat(pageable ->
                pageable.getPageSize() == 20
        ));
    }

    @Test
    @DisplayName("size 기본값 설정 테스트 - 음수 요청")
    void t9() {
        Long roomId = 1L;
        int page = 0;
        int requestedSize = -5;
        LocalDateTime before = null;

        List<RoomChatMessage> messages = Arrays.asList(testMessage);
        Page<RoomChatMessage> messagePage = new PageImpl<>(messages, PageRequest.of(page, 20), 1);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
        given(roomChatMessageRepository.findMessagesByRoomId(eq(roomId), any(Pageable.class)))
                .willReturn(messagePage);

        RoomChatPageResponse result = roomChatService.getRoomChatHistory(roomId, page, requestedSize, before);

        assertThat(result).isNotNull();

        // size가 기본값 20으로 설정되었는지 확인
        verify(roomChatMessageRepository).findMessagesByRoomId(eq(roomId), argThat(pageable ->
                pageable.getPageSize() == 20
        ));
    }

    @Test
    @DisplayName("size 정상 범위 테스트")
    void t10() {
        Long roomId = 1L;
        int page = 0;
        int requestedSize = 50; // 유효한 size
        LocalDateTime before = null;

        List<RoomChatMessage> messages = Arrays.asList(testMessage);
        Page<RoomChatMessage> messagePage = new PageImpl<>(messages, PageRequest.of(page, 50), 1);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
        given(roomChatMessageRepository.findMessagesByRoomId(eq(roomId), any(Pageable.class)))
                .willReturn(messagePage);

        RoomChatPageResponse result = roomChatService.getRoomChatHistory(roomId, page, requestedSize, before);

        assertThat(result).isNotNull();

        // 요청한 size가 그대로 유지되는지 확인
        verify(roomChatMessageRepository).findMessagesByRoomId(eq(roomId), argThat(pageable ->
                pageable.getPageSize() == 50
        ));
    }

    @Test
    @DisplayName("빈 채팅 기록 조회")
    void t11() {
        Long roomId = 1L;
        Page<RoomChatMessage> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
        given(roomChatMessageRepository.findMessagesByRoomId(eq(roomId), any(Pageable.class)))
                .willReturn(emptyPage);

        RoomChatPageResponse result = roomChatService.getRoomChatHistory(roomId, 0, 10, null);

        assertThat(result).isNotNull();
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("convertToDto 메소드 테스트")
    void t12() throws Exception {
        // ChatService의 private 메소드에 접근하기 위해 리플렉션 사용
        java.lang.reflect.Method convertToDtoMethod = RoomChatService.class.getDeclaredMethod("convertToDto", RoomChatMessage.class);
        convertToDtoMethod.setAccessible(true);

        RoomChatMessageDto result = (RoomChatMessageDto) convertToDtoMethod.invoke(roomChatService, testMessage);

        assertThat(result).isNotNull();

        assertThat(result.messageId()).isEqualTo(1L);
        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.nickname()).isEqualTo("테스터");
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(result.content()).isEqualTo("테스트 메시지");
        assertThat(result.messageType()).isEqualTo("TEXT");
        assertThat(result.attachment()).isNull();
        assertThat(result.createdAt()).isNotNull();
    }
}