package com.back.domain.chat.room.service;

import com.back.domain.chat.room.dto.ChatClearedNotification;
import com.back.domain.chat.room.dto.RoomChatMessageRequest;
import com.back.domain.chat.room.dto.RoomChatMessageResponse;
import com.back.domain.chat.room.dto.RoomChatPageResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.repository.RoomChatMessageRepository;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomChatService 테스트")
class RoomChatServiceTest {

    @Mock
    private RoomChatMessageRepository roomChatMessageRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @InjectMocks
    private RoomChatService roomChatService;

    private User testUser;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).build();
        UserProfile testProfile = UserProfile.builder().nickname("테스터").profileImageUrl("url").build();
        testUser.setUserProfile(testProfile);

        testRoom = Room.builder().id(1L).build();
    }

    @Nested
    @DisplayName("saveRoomChatMessage 메서드")
    class SaveRoomChatMessage {

        @Test
        @DisplayName("성공 - 채팅 메시지를 저장한다")
        void t1() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            RoomChatMessageRequest request = new RoomChatMessageRequest("안녕하세요");

            RoomChatMessage mockMessage = new RoomChatMessage(testRoom, testUser, request.content());

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(roomChatMessageRepository.save(any(RoomChatMessage.class))).willReturn(mockMessage);

            // when
            RoomChatMessage result = roomChatService.saveRoomChatMessage(roomId, userId, request);

            // then
            assertThat(result.getContent()).isEqualTo("안녕하세요");
            assertThat(result.getUser().getId()).isEqualTo(userId);
            assertThat(result.getRoom().getId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 방이면 CustomException 발생")
        void t2() {
            // given
            Long nonExistentRoomId = 999L;
            given(roomRepository.findById(nonExistentRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomChatService.saveRoomChatMessage(nonExistentRoomId, 1L, new RoomChatMessageRequest("...")))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getRoomChatHistory 메서드")
    class GetRoomChatHistory {

        @Test
        @DisplayName("성공 - 채팅 기록을 페이지로 조회한다")
        void t1() {
            // given
            Long roomId = 1L;
            RoomChatMessage message = new RoomChatMessage(testRoom, testUser, "테스트 메시지");
            Page<RoomChatMessage> messagePage = new PageImpl<>(List.of(message));

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomChatMessageRepository.findMessagesByRoomId(eq(roomId), any(Pageable.class))).willReturn(messagePage);

            // when
            RoomChatPageResponse result = roomChatService.getRoomChatHistory(roomId, 0, 10, null);

            // then
            assertThat(result.content()).hasSize(1);
            RoomChatMessageResponse responseDto = result.content().get(0);
            assertThat(responseDto.content()).isEqualTo("테스트 메시지");
            assertThat(responseDto.nickname()).isEqualTo("테스터");
        }
    }

    @Nested
    @DisplayName("clearRoomChat 메서드")
    class ClearRoomChat {

        @Test
        @DisplayName("성공 - 방장 권한으로 채팅을 전체 삭제한다")
        void t1() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            RoomMember hostMember = RoomMember.createHost(testRoom, testUser);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(hostMember));

            // when
            ChatClearedNotification.ClearedByDto result = roomChatService.clearRoomChat(roomId, userId);

            // then
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.role()).isEqualTo("HOST");
            verify(roomChatMessageRepository).deleteAllByRoomId(roomId);
        }

        @Test
        @DisplayName("실패 - 권한이 없는 경우(일반 멤버) CustomException 발생")
        void t2() {
            // given
            Long roomId = 1L;
            Long userId = 1L;
            RoomMember member = RoomMember.createMember(testRoom, testUser);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> roomChatService.clearRoomChat(roomId, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_DELETE_FORBIDDEN);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 방이면 CustomException 발생")
        void t3() {
            // given
            Long nonExistentRoomId = 999L;
            given(roomRepository.findById(nonExistentRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomChatService.clearRoomChat(nonExistentRoomId, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
        }
    }
}