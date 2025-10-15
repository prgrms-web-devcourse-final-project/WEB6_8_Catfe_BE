package com.back.domain.studyroom.service;

import com.back.domain.studyroom.dto.RoomFavoriteResponse;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomFavorite;
import com.back.domain.studyroom.repository.RoomFavoriteRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.service.RoomParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomFavoriteService 테스트")
class RoomFavoriteServiceTest {

    @Mock
    private RoomFavoriteRepository favoriteRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomParticipantService roomParticipantService;

    @InjectMocks
    private RoomFavoriteService favoriteService;

    private User testUser;
    private Room testRoom;
    private RoomFavorite testFavorite;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        testRoom = Room.create(
                "테스트 방",
                "설명",
                false,
                null,
                10,
                testUser,
                null,
                true,
                null
        );
        setRoomId(testRoom, 1L);

        testFavorite = RoomFavorite.create(testUser, testRoom);
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

    @Test
    @DisplayName("즐겨찾기 추가 - 성공")
    void addFavorite_Success() {
        // given
        given(favoriteRepository.existsByUserIdAndRoomId(1L, 1L)).willReturn(false);
        given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(favoriteRepository.save(any(RoomFavorite.class))).willReturn(testFavorite);

        // when
        favoriteService.addFavorite(1L, 1L);

        // then
        verify(favoriteRepository, times(1)).existsByUserIdAndRoomId(1L, 1L);
        verify(favoriteRepository, times(1)).save(any(RoomFavorite.class));
    }

    @Test
    @DisplayName("즐겨찾기 추가 - 이미 존재하면 무시 (Idempotent)")
    void addFavorite_AlreadyExists_Ignore() {
        // given
        given(favoriteRepository.existsByUserIdAndRoomId(1L, 1L)).willReturn(true);

        // when
        favoriteService.addFavorite(1L, 1L);

        // then
        verify(favoriteRepository, times(1)).existsByUserIdAndRoomId(1L, 1L);
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("즐겨찾기 추가 - 존재하지 않는 방")
    void addFavorite_RoomNotFound() {
        // given
        given(favoriteRepository.existsByUserIdAndRoomId(1L, 999L)).willReturn(false);
        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteService.addFavorite(999L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("즐겨찾기 제거 - 성공")
    void removeFavorite_Success() {
        // given
        given(favoriteRepository.findByUserIdAndRoomId(1L, 1L))
                .willReturn(Optional.of(testFavorite));

        // when
        favoriteService.removeFavorite(1L, 1L);

        // then
        verify(favoriteRepository, times(1)).delete(testFavorite);
    }

    @Test
    @DisplayName("즐겨찾기 제거 - 존재하지 않음")
    void removeFavorite_NotFound() {
        // given
        given(favoriteRepository.findByUserIdAndRoomId(1L, 999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteService.removeFavorite(999L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("내 즐겨찾기 목록 조회 - 성공")
    void getMyFavorites_Success() {
        // given
        List<RoomFavorite> favorites = List.of(testFavorite);
        given(favoriteRepository.findByUserIdWithRoom(1L)).willReturn(favorites);
        given(roomParticipantService.getParticipantCounts(anyList()))
                .willReturn(Map.of(1L, 5L));

        // when
        List<RoomFavoriteResponse> result = favoriteService.getMyFavorites(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomId()).isEqualTo(1L);
        assertThat(result.get(0).getCurrentParticipants()).isEqualTo(5);
    }

    @Test
    @DisplayName("내 즐겨찾기 목록 조회 - 빈 목록")
    void getMyFavorites_Empty() {
        // given
        given(favoriteRepository.findByUserIdWithRoom(1L)).willReturn(List.of());

        // when
        List<RoomFavoriteResponse> result = favoriteService.getMyFavorites(1L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("즐겨찾기 여부 확인 - true")
    void isFavorite_True() {
        // given
        given(favoriteRepository.existsByUserIdAndRoomId(1L, 1L)).willReturn(true);

        // when
        boolean result = favoriteService.isFavorite(1L, 1L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("즐겨찾기 여부 확인 - false")
    void isFavorite_False() {
        // given
        given(favoriteRepository.existsByUserIdAndRoomId(1L, 999L)).willReturn(false);

        // when
        boolean result = favoriteService.isFavorite(999L, 1L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("여러 방의 즐겨찾기 여부 일괄 조회")
    void getFavoriteRoomIds_Success() {
        // given
        List<Long> roomIds = List.of(1L, 2L, 3L);
        Set<Long> favoriteIds = Set.of(1L, 3L);
        given(favoriteRepository.findFavoriteRoomIds(1L, roomIds)).willReturn(favoriteIds);

        // when
        Set<Long> result = favoriteService.getFavoriteRoomIds(roomIds, 1L);

        // then
        assertThat(result).containsExactlyInAnyOrder(1L, 3L);
    }
}
