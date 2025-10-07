package com.back.domain.notification.controller;

import com.back.domain.notification.dto.NotificationSettingDto.*;
import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationSettingService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/notification-settings")
@Tag(name = "Notification Setting API", description = "알림 설정 API")
public class NotificationSettingController {

    private final NotificationSettingService settingService;

    @Operation(summary = "알림 설정 조회", description = "사용자의 모든 알림 설정을 조회합니다.")
    @GetMapping
    public ResponseEntity<RsData<SettingsResponse>> getSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser) {

        SettingsResponse response = settingService.getUserSettings(currentUser.getUserId());
        return ResponseEntity.ok(RsData.success("알림 설정 조회 성공", response));
    }

    @Operation(summary = "알림 설정 일괄 변경", description = "여러 알림 설정을 한 번에 변경합니다.")
    @PutMapping
    public ResponseEntity<RsData<Void>> updateSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @RequestBody @Valid UpdateSettingsRequest request) {

        settingService.updateSettings(currentUser.getUserId(), request.settings());
        return ResponseEntity.ok(RsData.success(null));
    }

    @Operation(summary = "개별 알림 설정 토글", description = "특정 알림 타입의 활성화 상태를 토글합니다.")
    @PutMapping("/{type}")
    public ResponseEntity<RsData<Void>> toggleSetting(
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "알림 타입", example = "ROOM_JOIN")
            @PathVariable NotificationSettingType type) {

        settingService.toggleSetting(currentUser.getUserId(), type);
        return ResponseEntity.ok(RsData.success(null));
    }

    @Operation(summary = "전체 알림 ON/OFF", description = "모든 알림을 한 번에 활성화하거나 비활성화합니다.")
    @PutMapping("/all")
    public ResponseEntity<RsData<Void>> toggleAllSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "활성화 여부", example = "true")
            @RequestParam boolean enable) {

        settingService.toggleAllSettings(currentUser.getUserId(), enable);
        return ResponseEntity.ok(RsData.success(null));
    }
}