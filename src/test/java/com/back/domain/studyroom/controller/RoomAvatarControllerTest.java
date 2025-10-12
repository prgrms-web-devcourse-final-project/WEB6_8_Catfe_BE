package com.back.domain.studyroom.controller;

import com.back.domain.studyroom.dto.AvatarResponse;
import com.back.domain.studyroom.dto.UpdateAvatarRequest;
import com.back.domain.studyroom.entity.Avatar;
import com.back.domain.studyroom.service.AvatarService;
import com.back.global.common.dto.RsData;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomAvatarController 테스트")
class RoomAvatarControllerTest {

    @Mock
    private AvatarService avatarService;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private RoomAvatarController roomAvatarController;

    private List<AvatarResponse> avatarResponses;

    @BeforeEach
    void setUp() {
        avatarResponses = List.of(
                new AvatarResponse(1L, "검은 고양이", "/images/avatars/cat-black.png", 
                        "귀여운 검은 고양이", true, "CAT"),
                new AvatarResponse(2L, "하얀 고양이", "/images/avatars/cat-white.png", 
                        "우아한 하얀 고양이", true, "CAT"),
                new AvatarResponse(3L, "노란 고양이", "/images/avatars/cat-orange.png", 
                        "발랄한 노란 고양이", true, "CAT")
        );
    }

    @Test
    @DisplayName("아바타 목록 조회 API - 성공")
    void getAvatars_Success() {
        // given
        given(avatarService.getAvailableAvatars()).willReturn(avatarResponses);

        // when
        ResponseEntity<RsData<List<AvatarResponse>>> response = 
                roomAvatarController.getAvatars(1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("아바타 목록 조회 완료");
        assertThat(response.getBody().getData()).hasSize(3);
        assertThat(response.getBody().getData())
                .extracting(AvatarResponse::getName)
                .containsExactly("검은 고양이", "하얀 고양이", "노란 고양이");

        verify(avatarService, times(1)).getAvailableAvatars();
    }

    @Test
    @DisplayName("아바타 목록 조회 API - 빈 리스트 반환")
    void getAvatars_EmptyList() {
        // given
        given(avatarService.getAvailableAvatars()).willReturn(List.of());

        // when
        ResponseEntity<RsData<List<AvatarResponse>>> response = 
                roomAvatarController.getAvatars(1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    @DisplayName("아바타 변경 API - 성공 (VISITOR)")
    void updateMyAvatar_Visitor_Success() {
        // given
        given(currentUser.getUserId()).willReturn(100L);

        UpdateAvatarRequest request = new UpdateAvatarRequest(2L);

        doNothing().when(avatarService).updateRoomAvatar(1L, 100L, 2L);

        // when
        ResponseEntity<RsData<Void>> response = 
                roomAvatarController.updateMyAvatar(1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("아바타가 변경되었습니다");

        verify(currentUser, times(1)).getUserId();
        verify(avatarService, times(1)).updateRoomAvatar(1L, 100L, 2L);
    }

    @Test
    @DisplayName("아바타 변경 API - 성공 (MEMBER)")
    void updateMyAvatar_Member_Success() {
        // given
        given(currentUser.getUserId()).willReturn(100L);

        UpdateAvatarRequest request = new UpdateAvatarRequest(3L);

        doNothing().when(avatarService).updateRoomAvatar(1L, 100L, 3L);

        // when
        ResponseEntity<RsData<Void>> response = 
                roomAvatarController.updateMyAvatar(1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(avatarService, times(1)).updateRoomAvatar(1L, 100L, 3L);
    }

    @Test
    @DisplayName("아바타 변경 API - 다른 방에서는 다른 아바타 설정 가능")
    void updateMyAvatar_DifferentRooms() {
        // given
        given(currentUser.getUserId()).willReturn(100L);

        UpdateAvatarRequest request1 = new UpdateAvatarRequest(1L);
        UpdateAvatarRequest request2 = new UpdateAvatarRequest(3L);

        // when
        roomAvatarController.updateMyAvatar(1L, request1); // 방1에서 아바타 1
        roomAvatarController.updateMyAvatar(2L, request2); // 방2에서 아바타 3

        // then
        verify(avatarService, times(1)).updateRoomAvatar(1L, 100L, 1L);
        verify(avatarService, times(1)).updateRoomAvatar(2L, 100L, 3L);
    }

    @Test
    @DisplayName("아바타 변경 API - 같은 아바타로 여러 번 변경 가능")
    void updateMyAvatar_SameAvatarMultipleTimes() {
        // given
        given(currentUser.getUserId()).willReturn(100L);

        UpdateAvatarRequest request = new UpdateAvatarRequest(2L);

        // when
        roomAvatarController.updateMyAvatar(1L, request);
        roomAvatarController.updateMyAvatar(1L, request);

        // then
        verify(avatarService, times(2)).updateRoomAvatar(1L, 100L, 2L);
    }

    @Test
    @DisplayName("아바타 변경 API - 여러 사용자가 동시에 변경 가능")
    void updateMyAvatar_MultipleUsers() {
        // given
        UpdateAvatarRequest request1 = new UpdateAvatarRequest(1L);
        UpdateAvatarRequest request2 = new UpdateAvatarRequest(2L);

        // when
        given(currentUser.getUserId()).willReturn(100L);
        roomAvatarController.updateMyAvatar(1L, request1);

        given(currentUser.getUserId()).willReturn(200L);
        roomAvatarController.updateMyAvatar(1L, request2);

        // then
        verify(avatarService, times(1)).updateRoomAvatar(1L, 100L, 1L);
        verify(avatarService, times(1)).updateRoomAvatar(1L, 200L, 2L);
    }

    @Test
    @DisplayName("아바타 목록 조회 - 카테고리 정보 포함")
    void getAvatars_IncludesCategory() {
        // given
        given(avatarService.getAvailableAvatars()).willReturn(avatarResponses);

        // when
        ResponseEntity<RsData<List<AvatarResponse>>> response = 
                roomAvatarController.getAvatars(1L);

        // then
        assertThat(response.getBody().getData())
                .allMatch(avatar -> avatar.getCategory() != null);
        assertThat(response.getBody().getData())
                .allMatch(avatar -> avatar.getCategory().equals("CAT"));
    }

    @Test
    @DisplayName("아바타 목록 조회 - isDefault 정보 포함")
    void getAvatars_IncludesIsDefault() {
        // given
        given(avatarService.getAvailableAvatars()).willReturn(avatarResponses);

        // when
        ResponseEntity<RsData<List<AvatarResponse>>> response = 
                roomAvatarController.getAvatars(1L);

        // then
        assertThat(response.getBody().getData())
                .allMatch(AvatarResponse::isDefault);
    }

    @Test
    @DisplayName("아바타 변경 API - 요청 검증 (avatarId 필수)")
    void updateMyAvatar_RequestValidation() {
        // given
        given(currentUser.getUserId()).willReturn(100L);

        // avatarId가 있는 정상 요청
        UpdateAvatarRequest validRequest = new UpdateAvatarRequest(1L);

        // when
        ResponseEntity<RsData<Void>> response = 
                roomAvatarController.updateMyAvatar(1L, validRequest);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(avatarService, times(1)).updateRoomAvatar(1L, 100L, 1L);
    }
}
