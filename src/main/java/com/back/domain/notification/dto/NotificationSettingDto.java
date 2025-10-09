package com.back.domain.notification.dto;

import com.back.domain.notification.entity.NotificationSetting;
import com.back.domain.notification.entity.NotificationSettingType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public class NotificationSettingDto {

    // 응답 DTO - 단일 설정
    @Schema(description = "알림 설정 정보")
    public record SettingInfo(
            @Schema(description = "알림 타입", example = "ROOM_JOIN")
            NotificationSettingType type,

            @Schema(description = "활성화 여부", example = "true")
            boolean enabled
    ) {
        public static SettingInfo from(NotificationSetting setting) {
            return new SettingInfo(
                    setting.getType(),
                    setting.isEnabled()
            );
        }
    }

    // 응답 DTO - 전체 설정
    @Schema(description = "사용자 알림 설정 전체 응답")
    public record SettingsResponse(
            @Schema(description = "전체 알림 켜기/끄기", example = "true")
            boolean allEnabled,

            @Schema(description = "개별 알림 설정 목록")
            List<SettingInfo> settings
    ) {
        public static SettingsResponse of(boolean allEnabled, List<NotificationSetting> settings) {
            return new SettingsResponse(
                    allEnabled,
                    settings.stream()
                            .map(SettingInfo::from)
                            .toList()
            );
        }
    }

    // 요청 DTO - 일괄 변경
    @Schema(description = "알림 설정 일괄 변경 요청")
    public record UpdateSettingsRequest(
            @Schema(description = "알림 타입별 활성화 여부",
                    example = "{\"ROOM_JOIN\": true, \"POST_COMMENT\": false}")
            Map<NotificationSettingType, Boolean> settings
    ) {}

    // 요청 DTO - 전체 ON/OFF
    @Schema(description = "전체 알림 ON/OFF 요청")
    public record ToggleAllRequest(
            @Schema(description = "전체 활성화 여부", example = "true")
            boolean enable
    ) {}
}