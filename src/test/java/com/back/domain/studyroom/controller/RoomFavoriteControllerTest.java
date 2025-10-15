package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.RoomFavoriteResponse;
import com.back.domain.studyroom.entity.RoomStatus;
import com.back.domain.studyroom.service.RoomFavoriteService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomFavoriteController 테스트")
class RoomFavoriteControllerTest {

    @Mock
    private RoomFavoriteService favoriteService;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private RoomFavoriteController controller;

    @Test
    @DisplayName("즐겨찾기 추가 - 성공")
    void addFavorite_Success() {
        // given
        Long userId = 1L;
        Long roomId = 1L;
        given(currentUser.getUserId()).willReturn(userId);
        doNothing().when(favoriteService).addFavorite(roomId, userId);

        // when
        ResponseEntity<?> response = controller.addFavorite(roomId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(favoriteService, times(1)).addFavorite(roomId, userId);
    }

    @Test
    @DisplayName("즐겨찾기 추가 - 존재하지 않는 방")
    void addFavorite_RoomNotFound() {
        // given
        Long userId = 1L;
        Long roomId = 999L;
        given(currentUser.getUserId()).willReturn(userId);
        doThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND))
                .when(favoriteService).addFavorite(roomId, userId);

        // when & then
        assertThatThrownBy(() -> controller.addFavorite(roomId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("즐겨찾기 제거 - 성공")
    void removeFavorite_Success() {
        // given
        Long userId = 1L;
        Long roomId = 1L;
        given(currentUser.getUserId()).willReturn(userId);
        doNothing().when(favoriteService).removeFavorite(roomId, userId);

        // when
        ResponseEntity<?> response = controller.removeFavorite(roomId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(favoriteService, times(1)).removeFavorite(roomId, userId);
    }

    @Test
    @DisplayName("즐겨찾기 제거 - 존재하지 않음")
    void removeFavorite_NotFound() {
        // given
        Long userId = 1L;
        Long roomId = 999L;
        given(currentUser.getUserId()).willReturn(userId);
        doThrow(new CustomException(ErrorCode.NOT_FOUND))
                .when(favoriteService).removeFavorite(roomId, userId);

        // when & then
        assertThatThrownBy(() -> controller.removeFavorite(roomId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("내 즐겨찾기 목록 조회 - 성공")
    void getMyFavorites_Success() {
        // given
        Long userId = 1L;
        List<RoomFavoriteResponse> favorites = List.of(
                RoomFavoriteResponse.builder()
                        .roomId(1L)
                        .title("테스트 방 1")
                        .currentParticipants(5)
                        .maxParticipants(10)
                        .status(RoomStatus.ACTIVE)
                        .favoritedAt(LocalDateTime.now())
                        .build(),
                RoomFavoriteResponse.builder()
                        .roomId(2L)
                        .title("테스트 방 2")
                        .currentParticipants(3)
                        .maxParticipants(8)
                        .status(RoomStatus.WAITING)
                        .favoritedAt(LocalDateTime.now())
                        .build()
        );

        given(currentUser.getUserId()).willReturn(userId);
        given(favoriteService.getMyFavorites(userId)).willReturn(favorites);

        // when
        ResponseEntity<?> response = controller.getMyFavorites();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(favoriteService, times(1)).getMyFavorites(userId);
    }

    @Test
    @DisplayName("내 즐겨찾기 목록 조회 - 빈 목록")
    void getMyFavorites_Empty() {
        // given
        Long userId = 1L;
        given(currentUser.getUserId()).willReturn(userId);
        given(favoriteService.getMyFavorites(userId)).willReturn(List.of());

        // when
        ResponseEntity<?> response = controller.getMyFavorites();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(favoriteService, times(1)).getMyFavorites(userId);
    }

    @Test
    @DisplayName("즐겨찾기 여부 확인 - true")
    void isFavorite_True() {
        // given
        Long userId = 1L;
        Long roomId = 1L;
        given(currentUser.getUserId()).willReturn(userId);
        given(favoriteService.isFavorite(roomId, userId)).willReturn(true);

        // when
        ResponseEntity<?> response = controller.isFavorite(roomId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(favoriteService, times(1)).isFavorite(roomId, userId);
    }

    @Test
    @DisplayName("즐겨찾기 여부 확인 - false")
    void isFavorite_False() {
        // given
        Long userId = 1L;
        Long roomId = 999L;
        given(currentUser.getUserId()).willReturn(userId);
        given(favoriteService.isFavorite(roomId, userId)).willReturn(false);

        // when
        ResponseEntity<?> response = controller.isFavorite(roomId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(favoriteService, times(1)).isFavorite(roomId, userId);
    }
}
