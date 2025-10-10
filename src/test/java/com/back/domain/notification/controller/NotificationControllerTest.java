package com.back.domain.notification.controller;

import com.back.domain.notification.dto.NotificationCreateRequest;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.security.user.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoomRepository roomRepository;

    private User receiver;
    private User actor;
    private Notification notification;
    private CustomUserDetails mockUser;

    @BeforeEach
    void setUp() {
        receiver = User.builder()
                .email("receiver@test.com")
                .username("수신자")
                .password("password123")
                .role(Role.USER)
                .build();

        actor = User.builder()
                .email("actor@test.com")
                .username("발신자")
                .password("password123")
                .role(Role.USER)
                .build();

        notification = Notification.createPersonalNotification(
                receiver, actor, "테스트 알림", "내용", "/target"
        );

        // JWT Mock 설정
        mockUser = CustomUserDetails.builder()
                .userId(1L)
                .username("testuser")
                .role(Role.USER)
                .build();

        given(jwtTokenProvider.validateAccessToken("faketoken")).willReturn(true);
        given(jwtTokenProvider.getAuthentication("faketoken"))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                ));
    }

    @Nested
    @DisplayName("알림 전송")
    class CreateNotificationTest {

        @Test
        @DisplayName("개인 알림 생성 성공")
        void t1() throws Exception {
            // given
            NotificationCreateRequest request = new NotificationCreateRequest(
                    "USER", 1L, 2L, "개인 알림", "내용", "/target"
            );

            given(userRepository.findById(1L)).willReturn(Optional.of(receiver));
            given(userRepository.findById(2L)).willReturn(Optional.of(actor));
            given(notificationService.createPersonalNotification(any(), any(), any(), any(), any(), any()))
                    .willReturn(notification);

            // when
            ResultActions result = mockMvc.perform(post("/api/notifications")
                    .header("Authorization", "Bearer faketoken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("알림 목록 조회")
    class GetNotificationsTest {

        @Test
        @DisplayName("알림 목록 조회 성공")
        void t1() throws Exception {
            // given
            Page<Notification> notificationPage = new PageImpl<>(List.of(notification));

            given(notificationService.getUserNotifications(anyLong(), any(Pageable.class)))
                    .willReturn(notificationPage);
            given(notificationService.getUnreadCount(anyLong())).willReturn(3L);
            given(notificationService.isNotificationRead(anyLong(), anyLong())).willReturn(false);

            // when
            ResultActions result = mockMvc.perform(get("/api/notifications")
                    .header("Authorization", "Bearer faketoken")
                    .param("page", "0")
                    .param("size", "20"));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("읽지 않은 알림만 조회")
        void t2() throws Exception {
            // given
            Page<Notification> notificationPage = new PageImpl<>(List.of(notification));

            given(notificationService.getUnreadNotifications(anyLong(), any(Pageable.class)))
                    .willReturn(notificationPage);
            given(notificationService.getUnreadCount(anyLong())).willReturn(1L);
            given(notificationService.isNotificationRead(anyLong(), anyLong())).willReturn(false);

            // when
            ResultActions result = mockMvc.perform(get("/api/notifications")
                    .header("Authorization", "Bearer faketoken")
                    .param("unreadOnly", "true"));

            // then
            result.andExpect(status().isOk());
            verify(notificationService).getUnreadNotifications(anyLong(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("알림 읽음 처리")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림 읽음 처리 성공")
        void t1() throws Exception {
            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(receiver));
            given(notificationService.getNotification(1L)).willReturn(notification);
            given(notificationService.isNotificationRead(1L, receiver.getId())).willReturn(true);

            // when
            ResultActions result = mockMvc.perform(put("/api/notifications/1/read")
                    .header("Authorization", "Bearer faketoken"));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(notificationService).markAsRead(eq(1L), any(User.class));
        }

        @Test
        @DisplayName("존재하지 않는 알림 - 404 에러")
        void t2() throws Exception {
            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(receiver));
            given(notificationService.getNotification(999L))
                    .willThrow(new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

            // when
            ResultActions result = mockMvc.perform(put("/api/notifications/999/read")
                    .header("Authorization", "Bearer faketoken"));

            // then
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("이미 읽은 알림 - 400 에러")
        void t3() throws Exception {
            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(receiver));
            willThrow(new CustomException(ErrorCode.NOTIFICATION_ALREADY_READ))
                    .given(notificationService).markAsRead(eq(1L), any(User.class));

            // when
            ResultActions result = mockMvc.perform(put("/api/notifications/1/read")
                    .header("Authorization", "Bearer faketoken"));

            // then
            result.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("전체 읽음 처리")
    class MarkAllAsReadTest {

        @Test
        @DisplayName("전체 알림 읽음 처리 성공")
        void t1() throws Exception {
            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(receiver));

            // when
            ResultActions result = mockMvc.perform(put("/api/notifications/read-all")
                    .header("Authorization", "Bearer faketoken"));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(notificationService).markMultipleAsRead(anyLong(), any(User.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 - 404 에러")
        void t2() throws Exception {
            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            ResultActions result = mockMvc.perform(put("/api/notifications/read-all")
                    .header("Authorization", "Bearer faketoken"));

            // then
            result.andExpect(status().isNotFound());
        }
    }
}