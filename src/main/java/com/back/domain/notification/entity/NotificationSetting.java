package com.back.domain.notification.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type"})
)
public class NotificationSetting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationSettingType type;

    @Column(nullable = false)
    private boolean enabled;

    public NotificationSetting(User user, NotificationSettingType type, boolean enabled) {
        this.user = user;
        this.type = type;
        this.enabled = enabled;
    }

    // 알림 설정 생성 (기본 활성화 상태)
    public static NotificationSetting create(User user, NotificationSettingType type) {
        return new NotificationSetting(user, type, true);
    }

    // 알림 설정 생성 (활성화 여부 직접 지정)
    public static NotificationSetting create(User user, NotificationSettingType type, boolean enabled) {
        return new NotificationSetting(user, type, enabled);
    }

    // 알림 설정 토글
    public void toggle() {
        this.enabled = !this.enabled;
    }

    // 알림 활성화
    public void enable() {
        this.enabled = true;
    }

    // 알림 비활성화
    public void disable() {
        this.enabled = false;
    }

}