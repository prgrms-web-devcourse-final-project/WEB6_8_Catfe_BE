package com.back.domain.studyroom.service;

import com.back.domain.studyroom.dto.GuestbookReactionSummary;
import com.back.domain.studyroom.dto.GuestbookResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomGuestbook;
import com.back.domain.studyroom.entity.RoomGuestbookPin;
import com.back.domain.studyroom.entity.RoomGuestbookReaction;
import com.back.domain.studyroom.repository.RoomGuestbookPinRepository;
import com.back.domain.studyroom.repository.RoomGuestbookReactionRepository;
import com.back.domain.studyroom.repository.RoomGuestbookRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.Role;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomGuestbookService 테스트")
class RoomGuestbookServiceTest {

    @Mock
    private RoomGuestbookRepository guestbookRepository;

    @Mock
    private RoomGuestbookReactionRepository reactionRepository;

    @Mock
    private RoomGuestbookPinRepository pinRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoomGuestbookService guestbookService;

    private User testUser;
    private Room testRoom;
    private RoomGuestbook testGuestbook;

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
                null,
                true,
                null
        );

        // 테스트 방명록 생성
        testGuestbook = RoomGuestbook.create(testRoom, testUser, "테스트 방명록입니다");
    }

    @Test
    @DisplayName("방명록 생성 - 성공")
    void createGuestbook_Success() {
        // given
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(guestbookRepository.save(any(RoomGuestbook.class))).willReturn(testGuestbook);

        // when
        GuestbookResponse response = guestbookService.createGuestbook(1L, "테스트 방명록입니다", 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("테스트 방명록입니다");
        assertThat(response.getNickname()).isEqualTo("테스트유저");
        assertThat(response.getIsAuthor()).isTrue();
        assertThat(response.getIsPinned()).isFalse();

        verify(guestbookRepository, times(1)).save(any(RoomGuestbook.class));
    }

    @Test
    @DisplayName("방명록 생성 - 방 없음 실패")
    void createGuestbook_RoomNotFound() {
        // given
        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> guestbookService.createGuestbook(999L, "테스트", 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("방명록 목록 조회 - 성공")
    void getGuestbooks_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<RoomGuestbook> guestbookPage = new PageImpl<>(Arrays.asList(testGuestbook), pageable, 1);

        given(roomRepository.existsById(1L)).willReturn(true);
        given(guestbookRepository.findByRoomIdWithUserOrderByPin(eq(1L), eq(1L), any())).willReturn(guestbookPage);
        given(pinRepository.findPinnedGuestbookIdsByUserIdAndRoomId(1L, 1L)).willReturn(Collections.emptySet());
        given(reactionRepository.findByGuestbookIdWithUser(any())).willReturn(Collections.emptyList());

        // when
        Page<GuestbookResponse> result = guestbookService.getGuestbooks(1L, 1L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("테스트 방명록입니다");
    }

    @Test
    @DisplayName("방명록 단건 조회 - 성공")
    void getGuestbook_Success() {
        // given
        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));
        given(reactionRepository.findByGuestbookIdWithUser(1L)).willReturn(Collections.emptyList());
        given(pinRepository.findByGuestbookIdAndUserId(1L, 1L)).willReturn(Optional.empty());

        // when
        GuestbookResponse response = guestbookService.getGuestbook(1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("테스트 방명록입니다");
        assertThat(response.getIsAuthor()).isTrue();
        assertThat(response.getIsPinned()).isFalse();
    }

    @Test
    @DisplayName("방명록 수정 - 성공")
    void updateGuestbook_Success() {
        // given
        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));
        given(reactionRepository.findByGuestbookIdWithUser(1L)).willReturn(Collections.emptyList());

        // when
        GuestbookResponse response = guestbookService.updateGuestbook(1L, "수정된 내용", 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(testGuestbook.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("방명록 수정 - 권한 없음 실패")
    void updateGuestbook_NoPermission() {
        // given
        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));

        // when & then
        assertThatThrownBy(() -> guestbookService.updateGuestbook(1L, "수정", 999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUESTBOOK_NO_PERMISSION);
    }

    @Test
    @DisplayName("방명록 삭제 - 성공")
    void deleteGuestbook_Success() {
        // given
        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));

        // when
        guestbookService.deleteGuestbook(1L, 1L);

        // then
        verify(guestbookRepository, times(1)).delete(testGuestbook);
    }

    @Test
    @DisplayName("방명록 삭제 - 권한 없음 실패")
    void deleteGuestbook_NoPermission() {
        // given
        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));

        // when & then
        assertThatThrownBy(() -> guestbookService.deleteGuestbook(1L, 999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GUESTBOOK_NO_PERMISSION);
    }

    @Test
    @DisplayName("이모지 반응 추가 - 성공")
    void toggleReaction_Add_Success() {
        // given
        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(reactionRepository.findByGuestbookIdAndUserIdAndEmoji(1L, 1L, "👍")).willReturn(Optional.empty());
        given(reactionRepository.findByGuestbookIdWithUser(1L)).willReturn(Collections.emptyList());

        // when
        GuestbookResponse response = guestbookService.toggleReaction(1L, "👍", 1L);

        // then
        assertThat(response).isNotNull();
        verify(reactionRepository, times(1)).save(any(RoomGuestbookReaction.class));
    }

    @Test
    @DisplayName("이모지 반응 제거 - 성공")
    void toggleReaction_Remove_Success() {
        // given
        RoomGuestbookReaction reaction = RoomGuestbookReaction.create(testGuestbook, testUser, "👍");

        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(reactionRepository.findByGuestbookIdAndUserIdAndEmoji(1L, 1L, "👍"))
                .willReturn(Optional.of(reaction));
        given(reactionRepository.findByGuestbookIdWithUser(1L)).willReturn(Collections.emptyList());

        // when
        GuestbookResponse response = guestbookService.toggleReaction(1L, "👍", 1L);

        // then
        assertThat(response).isNotNull();
        verify(reactionRepository, times(1)).delete(reaction);
    }

    @Test
    @DisplayName("이모지 반응 요약 - 여러 사용자 반응")
    void buildReactionSummaries_MultipleUsers() {
        // given
        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@test.com")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        UserProfile userProfile2 = new UserProfile();
        userProfile2.setNickname("사용자2");
        user2.setUserProfile(userProfile2);

        RoomGuestbookReaction reaction1 = RoomGuestbookReaction.create(testGuestbook, testUser, "👍");
        RoomGuestbookReaction reaction2 = RoomGuestbookReaction.create(testGuestbook, user2, "👍");
        RoomGuestbookReaction reaction3 = RoomGuestbookReaction.create(testGuestbook, testUser, "❤️");

        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));
        given(reactionRepository.findByGuestbookIdWithUser(1L))
                .willReturn(Arrays.asList(reaction1, reaction2, reaction3));
        given(pinRepository.findByGuestbookIdAndUserId(1L, 1L)).willReturn(Optional.empty());

        // when
        GuestbookResponse response = guestbookService.getGuestbook(1L, 1L);

        // then
        assertThat(response.getReactions()).hasSize(2);
        
        // 👍 반응 확인 (count 2)
        GuestbookReactionSummary thumbsUp = response.getReactions().stream()
                .filter(r -> r.getEmoji().equals("👍"))
                .findFirst()
                .orElse(null);
        
        assertThat(thumbsUp).isNotNull();
        assertThat(thumbsUp.getCount()).isEqualTo(2L);
        assertThat(thumbsUp.getReactedByMe()).isTrue();
        
        // ❤️ 반응 확인 (count 1)
        GuestbookReactionSummary heart = response.getReactions().stream()
                .filter(r -> r.getEmoji().equals("❤️"))
                .findFirst()
                .orElse(null);
        
        assertThat(heart).isNotNull();
        assertThat(heart.getCount()).isEqualTo(1L);
        assertThat(heart.getReactedByMe()).isTrue();
    }

    @Test
    @DisplayName("방명록 핀 추가 - 성공")
    void togglePin_Add_Success() {
        // given
        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(pinRepository.findByGuestbookIdAndUserId(1L, 1L)).willReturn(Optional.empty());
        given(reactionRepository.findByGuestbookIdWithUser(1L)).willReturn(Collections.emptyList());

        // when
        GuestbookResponse response = guestbookService.togglePin(1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsPinned()).isTrue();
        verify(pinRepository, times(1)).save(any(RoomGuestbookPin.class));
    }

    @Test
    @DisplayName("방명록 핀 제거 - 성공")
    void togglePin_Remove_Success() {
        // given
        RoomGuestbookPin pin = RoomGuestbookPin.create(testGuestbook, testUser);

        given(guestbookRepository.findByIdWithUserAndRoom(1L)).willReturn(Optional.of(testGuestbook));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(pinRepository.findByGuestbookIdAndUserId(1L, 1L)).willReturn(Optional.of(pin));
        given(reactionRepository.findByGuestbookIdWithUser(1L)).willReturn(Collections.emptyList());

        // when
        GuestbookResponse response = guestbookService.togglePin(1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsPinned()).isFalse();
        verify(pinRepository, times(1)).delete(pin);
    }
}
