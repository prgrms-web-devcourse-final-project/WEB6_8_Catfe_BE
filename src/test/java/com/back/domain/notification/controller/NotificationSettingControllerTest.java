package com.back.domain.notification.controller;

import com.back.domain.notification.dto.NotificationSettingDto.*;
import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationSettingService;
import com.back.domain.user.common.enums.Role;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("NotificationSettingController 테스트")
class NotificationSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationSettingService settingService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private String accessToken;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        accessToken = "Bearer test-access-token";

        CustomUserDetails userDetails = new CustomUserDetails(userId, "testuser", Role.USER);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );

        given(jwtTokenProvider.getAuthentication(anyString())).willReturn(authentication);
        given(jwtTokenProvider.validateAccessToken(anyString())).willReturn(true);
    }

    @Nested
    @DisplayName("알림 설정 조회 API")
    class GetSettingsTest {

        @Test
        @DisplayName("알림 설정 조회 성공")
        void t1() throws Exception {
            // given
            SettingsResponse mockResponse = new SettingsResponse(
                    true,
                    List.of(
                            new SettingInfo(NotificationSettingType.SYSTEM, true),
                            new SettingInfo(NotificationSettingType.ROOM_JOIN, true),
                            new SettingInfo(NotificationSettingType.ROOM_NOTICE, true),
                            new SettingInfo(NotificationSettingType.POST_COMMENT, true),
                            new SettingInfo(NotificationSettingType.POST_LIKE, true)
                    )
            );

            given(settingService.getUserSettings(userId)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/users/me/notification-settings")
                            .header("Authorization", "Bearer faketoken"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 설정 조회 성공"))
                    .andExpect(jsonPath("$.data.allEnabled").value(true))
                    .andExpect(jsonPath("$.data.settings").isArray())
                    .andExpect(jsonPath("$.data.settings.length()").value(5));

            verify(settingService).getUserSettings(userId);
        }

        @Test
        @DisplayName("알림 설정 조회 - 인증 토큰 없음")
        void t2() throws Exception {
            // when & then
            mockMvc.perform(get("/api/users/me/notification-settings"))
                    .andExpect(status().isUnauthorized());

            verify(settingService, never()).getUserSettings(anyLong());
        }

        @Test
        @DisplayName("알림 설정 조회 - 일부 설정이 비활성화된 경우")
        void t3() throws Exception {
            // given
            SettingsResponse mockResponse = new SettingsResponse(
                    false,
                    List.of(
                            new SettingInfo(NotificationSettingType.SYSTEM, true),
                            new SettingInfo(NotificationSettingType.ROOM_JOIN, false),
                            new SettingInfo(NotificationSettingType.ROOM_NOTICE, true),
                            new SettingInfo(NotificationSettingType.POST_COMMENT, false),
                            new SettingInfo(NotificationSettingType.POST_LIKE, true)
                    )
            );

            given(settingService.getUserSettings(userId)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/users/me/notification-settings")
                            .header("Authorization", "Bearer faketoken"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.allEnabled").value(false))
                    .andExpect(jsonPath("$.data.settings[1].enabled").value(false))
                    .andExpect(jsonPath("$.data.settings[3].enabled").value(false));

            verify(settingService).getUserSettings(userId);
        }
    }

    @Nested
    @DisplayName("개별 알림 설정 토글 API")
    class ToggleSettingTest {

        @Test
        @DisplayName("개별 알림 설정 토글 성공 - SYSTEM")
        void t1() throws Exception {
            // given
            willDoNothing().given(settingService)
                    .toggleSetting(userId, NotificationSettingType.SYSTEM);

            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/SYSTEM")
                            .header("Authorization", "Bearer faketoken"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(settingService).toggleSetting(userId, NotificationSettingType.SYSTEM);
        }

        @Test
        @DisplayName("개별 알림 설정 토글 성공 - POST_COMMENT")
        void t2() throws Exception {
            // given
            willDoNothing().given(settingService)
                    .toggleSetting(userId, NotificationSettingType.POST_COMMENT);

            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/POST_COMMENT")
                            .header("Authorization", "Bearer faketoken"))
                    .andExpect(status().isOk());

            verify(settingService).toggleSetting(userId, NotificationSettingType.POST_COMMENT);
        }

        @Test
        @DisplayName("개별 알림 설정 토글 - 잘못된 타입")
        void t3() throws Exception {
            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/INVALID_TYPE")
                            .header("Authorization", "Bearer faketoken"))
                    .andExpect(status().isBadRequest());

            verify(settingService, never()).toggleSetting(anyLong(), any());
        }

        @Test
        @DisplayName("개별 알림 설정 토글 - 설정이 없는 경우 예외")
        void t4() throws Exception {
            // given
            willThrow(new CustomException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND))
                    .given(settingService).toggleSetting(userId, NotificationSettingType.SYSTEM);

            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/SYSTEM")
                            .header("Authorization", "Bearer faketoken"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("NOTIFICATION_SETTING_001"));

            verify(settingService).toggleSetting(userId, NotificationSettingType.SYSTEM);
        }

        @Test
        @DisplayName("개별 알림 설정 토글 - 인증 토큰 없음")
        void t5() throws Exception {
            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/SYSTEM"))
                    .andExpect(status().isUnauthorized());

            verify(settingService, never()).toggleSetting(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("전체 알림 ON/OFF API")
    class ToggleAllSettingsTest {

        @Test
        @DisplayName("전체 알림 활성화 성공")
        void t1() throws Exception {
            // given
            willDoNothing().given(settingService).toggleAllSettings(userId, true);

            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/all")
                            .header("Authorization", "Bearer faketoken")
                            .param("enable", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(settingService).toggleAllSettings(userId, true);
        }

        @Test
        @DisplayName("전체 알림 비활성화 성공")
        void t2() throws Exception {
            // given
            willDoNothing().given(settingService).toggleAllSettings(userId, false);

            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/all")
                            .header("Authorization", "Bearer faketoken")
                            .param("enable", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(settingService).toggleAllSettings(userId, false);
        }

        @Test
        @DisplayName("전체 알림 ON/OFF - enable 파라미터 누락")
        void t3() throws Exception {
            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/all")
                            .header("Authorization", "Bearer faketoken"))
                    .andExpect(status().isBadRequest());

            verify(settingService, never()).toggleAllSettings(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("전체 알림 ON/OFF - 설정이 없는 경우 예외")
        void t4() throws Exception {
            // given
            willThrow(new CustomException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND))
                    .given(settingService).toggleAllSettings(userId, true);

            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/all")
                            .header("Authorization", "Bearer faketoken")
                            .param("enable", "true"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("NOTIFICATION_SETTING_001"));

            verify(settingService).toggleAllSettings(userId, true);
        }

        @Test
        @DisplayName("전체 알림 ON/OFF - 인증 토큰 없음")
        void t5() throws Exception {
            // when & then
            mockMvc.perform(put("/api/users/me/notification-settings/all")
                            .param("enable", "true"))
                    .andExpect(status().isUnauthorized());

            verify(settingService, never()).toggleAllSettings(anyLong(), anyBoolean());
        }
    }
}