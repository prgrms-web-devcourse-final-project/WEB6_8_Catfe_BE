package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.notification.entity.QNotification;
import com.back.domain.notification.entity.QNotificationRead;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 유저의 알림 목록 조회 (개인 알림 + 시스템 알림)
     * - 수신자가 해당 유저인 알림
     * - 또는 시스템 알림 (모든 유저에게 표시)
     */
    @Override
    public Page<Notification> findByUserIdOrSystemType(Long userId, Pageable pageable) {
        QNotification notification = QNotification.notification;

        List<Notification> content = queryFactory
                .selectFrom(notification)
                .where(
                        notification.receiver.id.eq(userId)
                                .or(notification.type.eq(NotificationType.SYSTEM))
                )
                .orderBy(notification.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(notification.count())
                .from(notification)
                .where(
                        notification.receiver.id.eq(userId)
                                .or(notification.type.eq(NotificationType.SYSTEM))
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 특정 유저의 읽지 않은 알림 개수 조회
     * - NotificationRead 테이블에 읽음 기록이 없는 알림만 카운트
     * - LEFT JOIN으로 읽음 기록 확인 후, 읽음 기록이 null인 경우만 집계
     */
    @Override
    public long countUnreadByUserId(Long userId) {
        QNotification notification = QNotification.notification;
        QNotificationRead notificationRead = QNotificationRead.notificationRead;

        Long count = queryFactory
                .select(notification.count())
                .from(notification)
                .leftJoin(notificationRead)
                .on(notification.id.eq(notificationRead.notification.id)
                        .and(notificationRead.user.id.eq(userId)))
                .where(
                        notification.receiver.id.eq(userId)  // ✨ user → receiver
                                .or(notification.type.eq(NotificationType.SYSTEM)),
                        notificationRead.id.isNull()
                )
                .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * 특정 유저의 읽지 않은 알림 목록 조회 (페이징)
     * - NotificationRead 테이블에 읽음 기록이 없는 알림만 조회
     * - LEFT JOIN 후 읽음 기록이 null인 알림만 반환
     */
    @Override
    public Page<Notification> findUnreadByUserId(Long userId, Pageable pageable) {
        QNotification notification = QNotification.notification;
        QNotificationRead notificationRead = QNotificationRead.notificationRead;

        List<Notification> content = queryFactory
                .selectFrom(notification)
                .leftJoin(notificationRead)
                .on(notification.id.eq(notificationRead.notification.id)
                        .and(notificationRead.user.id.eq(userId)))
                .where(
                        notification.receiver.id.eq(userId)
                                .or(notification.type.eq(NotificationType.SYSTEM)),
                        notificationRead.id.isNull()
                )
                .orderBy(notification.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(notification.count())
                .from(notification)
                .leftJoin(notificationRead)
                .on(notification.id.eq(notificationRead.notification.id)
                        .and(notificationRead.user.id.eq(userId)))
                .where(
                        notification.receiver.id.eq(userId)
                                .or(notification.type.eq(NotificationType.SYSTEM)),
                        notificationRead.id.isNull()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}