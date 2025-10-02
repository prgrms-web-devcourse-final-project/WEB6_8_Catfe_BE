package com.back.domain.notification.entity;

public enum NotificationSettingType {
    SYSTEM,          // 시스템 알림
    ROOM_JOIN,       // 스터디룸 입장 알림
    ROOM_NOTICE,     // 스터디룸 공지 알림
    POST_COMMENT,    // 게시글 댓글 알림
    POST_LIKE        // 게시글 좋아요 알림
}