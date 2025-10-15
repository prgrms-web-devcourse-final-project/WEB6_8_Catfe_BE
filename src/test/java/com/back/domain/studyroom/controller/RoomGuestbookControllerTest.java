package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.GuestbookResponse;
import com.back.domain.studyroom.service.RoomGuestbookService;
import com.back.global.security.user.CurrentUser;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomGuestbookController 테스트")
class RoomGuestbookControllerTest {

    @Mock
    private RoomGuestbookService guestbookService;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private RoomGuestbookController guestbookController;

    private GuestbookResponse testGuestbook;

    @BeforeEach
    void setUp() {
        testGuestbook = GuestbookResponse.builder()
                .guestbookId(1L)
                .userId(1L)
                .nickname("테스트유저")
                .profileImageUrl("https://example.com/profile.jpg")
                .content("테스트 방명록입니다")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isAuthor(true)
                .reactions(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("방명록 목록 조회 - 성공")
    void getGuestbooks_Success() {
        // given
        given(currentUser.getUserIdOrNull()).willReturn(1L);

        Page<GuestbookResponse> guestbookPage = new PageImpl<>(
                Arrays.asList(testGuestbook),
                PageRequest.of(0, 20),
                1
        );
        given(guestbookService.getGuestbooks(eq(1L), eq(1L), any())).willReturn(guestbookPage);

        // when
        ResponseEntity<?> response = guestbookController.getGuestbooks(1L, 0, 20);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(guestbookService, times(1)).getGuestbooks(eq(1L), eq(1L), any());
    }

    @Test
    @DisplayName("방명록 단건 조회 - 성공")
    void getGuestbook_Success() {
        // given
        given(currentUser.getUserIdOrNull()).willReturn(1L);
        given(guestbookService.getGuestbook(1L, 1L)).willReturn(testGuestbook);

        // when
        ResponseEntity<?> response = guestbookController.getGuestbook(1L, 1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(guestbookService, times(1)).getGuestbook(1L, 1L);
    }

    @Test
    @DisplayName("방명록 생성 - 성공")
    void createGuestbook_Success() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(guestbookService.createGuestbook(eq(1L), anyString(), eq(1L))).willReturn(testGuestbook);

        com.back.domain.studyroom.dto.CreateGuestbookRequest request = 
                new com.back.domain.studyroom.dto.CreateGuestbookRequest("테스트 방명록입니다");

        // when
        ResponseEntity<?> response = guestbookController.createGuestbook(1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(guestbookService, times(1)).createGuestbook(eq(1L), anyString(), eq(1L));
    }

    @Test
    @DisplayName("방명록 수정 - 성공")
    void updateGuestbook_Success() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(guestbookService.updateGuestbook(eq(1L), anyString(), eq(1L))).willReturn(testGuestbook);

        com.back.domain.studyroom.dto.UpdateGuestbookRequest request = 
                new com.back.domain.studyroom.dto.UpdateGuestbookRequest("수정된 내용");

        // when
        ResponseEntity<?> response = guestbookController.updateGuestbook(1L, 1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(guestbookService, times(1)).updateGuestbook(eq(1L), anyString(), eq(1L));
    }

    @Test
    @DisplayName("방명록 삭제 - 성공")
    void deleteGuestbook_Success() {
        // given
        given(currentUser.getUserId()).willReturn(1L);

        // when
        ResponseEntity<?> response = guestbookController.deleteGuestbook(1L, 1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(guestbookService, times(1)).deleteGuestbook(1L, 1L);
    }

    @Test
    @DisplayName("이모지 반응 토글 - 성공")
    void toggleReaction_Success() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(guestbookService.toggleReaction(eq(1L), eq("👍"), eq(1L))).willReturn(testGuestbook);

        com.back.domain.studyroom.dto.AddGuestbookReactionRequest request = 
                new com.back.domain.studyroom.dto.AddGuestbookReactionRequest("👍");

        // when
        ResponseEntity<?> response = guestbookController.toggleReaction(1L, 1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(guestbookService, times(1)).toggleReaction(eq(1L), eq("👍"), eq(1L));
    }

    @Test
    @DisplayName("방명록 핀 토글 - 성공")
    void togglePin_Success() {
        // given
        given(currentUser.getUserId()).willReturn(1L);
        given(guestbookService.togglePin(1L, 1L)).willReturn(testGuestbook);

        // when
        ResponseEntity<?> response = guestbookController.togglePin(1L, 1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(guestbookService, times(1)).togglePin(1L, 1L);
    }
}
