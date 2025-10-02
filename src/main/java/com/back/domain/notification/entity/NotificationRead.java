package com.back.domain.notification.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class NotificationRead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime readAt;

    public NotificationRead(Notification notification, User user) {
        this.notification = notification;
        this.user = user;
        this.readAt = LocalDateTime.now();
    }

    // 읽음 기록 생성
    public static NotificationRead create(Notification notification, User user) {
        return new NotificationRead(notification, user);
    }

    // 특정 시간 이후에 읽었는지 확인
    public boolean isReadAfter(LocalDateTime dateTime) {
        return readAt.isAfter(dateTime);
    }
}
