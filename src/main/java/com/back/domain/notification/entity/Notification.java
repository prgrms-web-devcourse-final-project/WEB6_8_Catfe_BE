package com.back.domain.notification.entity;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    private String targetUrl;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NotificationRead> notificationReads = new ArrayList<>();

    private Notification(NotificationType type, String title, String content, String targetUrl) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetUrl = targetUrl;
        this.status = NotificationStatus.UNREAD;
    }

    // 개인 알림 생성
    public static Notification createPersonalNotification(User user, String title, String content, String targetUrl) {
        Notification notification = new Notification(NotificationType.PERSONAL, title, content, targetUrl);
        notification.user = user;
        return notification;
    }

    // 스터디룸 알림 생성
    public static Notification createRoomNotification(Room room, String title, String content, String targetUrl) {
        Notification notification = new Notification(NotificationType.ROOM, title, content, targetUrl);
        notification.room = room;
        return notification;
    }

    // 시스템 알림 생성
    public static Notification createSystemNotification(String title, String content, String targetUrl) {
        return new Notification(NotificationType.SYSTEM, title, content, targetUrl);
    }

    // 커뮤니티 알림 생성
    public static Notification createCommunityNotification(User user, String title, String content, String targetUrl) {
        Notification notification = new Notification(NotificationType.COMMUNITY, title, content, targetUrl);
        notification.user = user;
        return notification;
    }

    // 알림을 읽음 상태로 변경
    public void markAsRead() {
        this.status = NotificationStatus.READ;
    }

    // 전체 알림인지 확인
    public boolean isSystemNotification() {
        return this.type == NotificationType.SYSTEM;
    }

    // 개인 알림인지 확인
    public boolean isPersonalNotification() {
        return this.type == NotificationType.PERSONAL;
    }

    // 특정 유저에게 표시되어야 하는 알림인지 확인
    public boolean isVisibleToUser(Long userId) {

        // 시스템 알림은 모두에게 표시
        if (isSystemNotification()) {
            return true;
        }

        // 개인 알림은 해당 유저에게만 표시
        return user != null && user.getId().equals(userId);
    }
}