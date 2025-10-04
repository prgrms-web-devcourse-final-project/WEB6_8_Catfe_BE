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
    @JoinColumn(name = "receiver_id")
    private User receiver; // 수신자 (누가 받는지)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;  // 발신자 (누가 이 활동을 했는지)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false)
    private String title; // 알림 제목 (필수)

    @Column(columnDefinition = "TEXT")
    private String content; // 알림 본문 또는 댓글 미리보기 (선택)

    private String targetUrl;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NotificationRead> notificationReads = new ArrayList<>();

    private Notification(NotificationType type, String title, String content, String targetUrl) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetUrl = targetUrl;
    }

    // 개인 알림 생성
    public static Notification createPersonalNotification(
            User receiver,
            User actor,
            String title,
            String content,
            String targetUrl) {
        Notification notification = new Notification(NotificationType.PERSONAL, title, content, targetUrl);
        notification.receiver = receiver;
        notification.actor = actor;
        return notification;
    }

    // 스터디룸 알림 생성
    public static Notification createRoomNotification(
            Room room,
            User actor,
            String title,
            String content,
            String targetUrl) {
        Notification notification = new Notification(NotificationType.ROOM, title, content, targetUrl);
        notification.room = room;
        notification.actor = actor;
        return notification;
    }

    // 시스템 알림 생성 (발신자 없음, 모두에게 전달)
    public static Notification createSystemNotification(
            String title,
            String content,
            String targetUrl) {
        return new Notification(NotificationType.SYSTEM, title, content, targetUrl);
    }

    // 커뮤니티 알림 생성 (댓글, 좋아요 등)
    public static Notification createCommunityNotification(
            User receiver,      // 수신자 (게시글 작성자)
            User actor,         // 발신자 (댓글 작성자)
            String title,
            String content,
            String targetUrl) {
        Notification notification = new Notification(NotificationType.COMMUNITY, title, content, targetUrl);
        notification.receiver = receiver;
        notification.actor = actor;
        return notification;
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

        // 개인/커뮤니티 알림은 수신자에게만 표시
        return receiver != null && receiver.getId().equals(userId);
    }

    // 특정 유저가 이 알림을 읽었는지 확인
    public boolean isReadBy(Long userId) {
        return notificationReads.stream()
                .anyMatch(read -> read.getUser().getId().equals(userId));
    }
}