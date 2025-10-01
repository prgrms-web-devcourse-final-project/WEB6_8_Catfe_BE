package com.back.global.websocket.webrtc.service;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.user.entity.User;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebRTC 시그널링 메세지 검증")
class WebRTCSignalValidatorTest {

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @InjectMocks
    private WebRTCSignalValidator validator;

    private Long roomId;
    private Long fromUserId;
    private Long targetUserId;
    private RoomMember onlineFromMember;
    private RoomMember onlineTargetMember;
    private RoomMember offlineFromMember;
    private RoomMember offlineTargetMember;

    @BeforeEach
    void setUp() {
        roomId = 1L;
        fromUserId = 10L;
        targetUserId = 20L;

        Room mockRoom = mock(Room.class);
        User fromUser = mock(User.class);
        User targetUser = mock(User.class);

        // 온라인/오프라인 구분은 Redis로 이관 예정
        // 현재는 멤버 존재 여부만 체크
        onlineFromMember = RoomMember.createMember(mockRoom, fromUser);
        onlineTargetMember = RoomMember.createMember(mockRoom, targetUser);
        offlineFromMember = RoomMember.createMember(mockRoom, fromUser);
        offlineTargetMember = RoomMember.createMember(mockRoom, targetUser);
    }

    @Nested
    @DisplayName("시그널 검증")
    class ValidateSignalTest {

        @Test
        @DisplayName("정상 - 모든 조건 만족")
        void t1() {
            // given
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, fromUserId))
                    .willReturn(Optional.of(onlineFromMember));
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId))
                    .willReturn(Optional.of(onlineTargetMember));

            // when & then
            assertThatCode(() -> validator.validateSignal(roomId, fromUserId, targetUserId))
                    .doesNotThrowAnyException();

            verify(roomMemberRepository).findByRoomIdAndUserId(roomId, fromUserId);
            verify(roomMemberRepository).findByRoomIdAndUserId(roomId, targetUserId);
        }

        @Test
        @DisplayName("실패 - 자기 자신에게 시그널 전송")
        void t2() {
            // given
            Long sameUserId = 10L;

            // when & then
            assertThatThrownBy(() -> validator.validateSignal(roomId, sameUserId, sameUserId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("실패 - 발신자가 방에 없음")
        void t3() {
            // given
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, fromUserId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> validator.validateSignal(roomId, fromUserId, targetUserId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_MEMBER);

            verify(roomMemberRepository).findByRoomIdAndUserId(roomId, fromUserId);
        }

        @Test
        @DisplayName("실패 - 수신자가 방에 없음")
        void t4() {
            // given
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, fromUserId))
                    .willReturn(Optional.of(onlineFromMember));
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> validator.validateSignal(roomId, fromUserId, targetUserId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_MEMBER);

            verify(roomMemberRepository).findByRoomIdAndUserId(roomId, fromUserId);
            verify(roomMemberRepository).findByRoomIdAndUserId(roomId, targetUserId);
        }
    }

    @Nested
    @DisplayName("미디어 상태 변경 검증")
    class ValidateMediaStateChangeTest {

        @Test
        @DisplayName("정상 - 멤버 존재")
        void t5() {
            // given
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, fromUserId))
                    .willReturn(Optional.of(onlineFromMember));

            // when & then
            assertThatCode(() -> validator.validateMediaStateChange(roomId, fromUserId))
                    .doesNotThrowAnyException();

            verify(roomMemberRepository).findByRoomIdAndUserId(roomId, fromUserId);
        }

        @Test
        @DisplayName("실패 - 방에 없는 사용자")
        void t6() {
            // given
            given(roomMemberRepository.findByRoomIdAndUserId(roomId, fromUserId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> validator.validateMediaStateChange(roomId, fromUserId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_MEMBER);

            verify(roomMemberRepository).findByRoomIdAndUserId(roomId, fromUserId);
        }

        @Test
        @DisplayName("정상 - 다른 방의 멤버")
        void t7() {
            // given
            Long differentRoomId = 999L;
            Room differentRoom = mock(Room.class);
            User user = mock(User.class);

            RoomMember memberInDifferentRoom = RoomMember.createMember(differentRoom, user);

            given(roomMemberRepository.findByRoomIdAndUserId(differentRoomId, fromUserId))
                    .willReturn(Optional.of(memberInDifferentRoom));

            // when & then
            assertThatCode(() -> validator.validateMediaStateChange(differentRoomId, fromUserId))
                    .doesNotThrowAnyException();

            verify(roomMemberRepository).findByRoomIdAndUserId(differentRoomId, fromUserId);
        }
    }
}