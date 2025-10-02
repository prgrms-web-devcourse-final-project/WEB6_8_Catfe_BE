package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.notification.entity.QNotification;
import com.back.domain.notification.entity.QNotificationRead;
import com.back.domain.studyroom.entity.QRoomMember;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Notification> findByUserIdOrSystemType(Long userId, Pageable pageable) {
        QNotification notification = QNotification.notification;
        QRoomMember roomMember = QRoomMember.roomMember;

        // user_id가 일치하는 알림 (PERSONAL + COMMUNITY)
        BooleanExpression condition = notification.user.id.eq(userId)
                // 시스템 알림 (모두에게)
                .or(notification.type.eq(NotificationType.SYSTEM))
                // 내가 멤버인 스터디룸의 알림
                .or(
                        notification.type.eq(NotificationType.ROOM)
                                .and(notification.room.id.in(
                                        JPAExpressions
                                                // 서브쿼리 : 내가 멤버인 방 ID들
                                                .select(roomMember.room.id)
                                                .from(roomMember)
                                                .where(roomMember.user.id.eq(userId))
                                ))
                );

        List<Notification> content = queryFactory
                .selectFrom(notification)
                .where(condition)
                .orderBy(notification.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(notification.count())
                .from(notification)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        QNotification notification = QNotification.notification;
        QNotificationRead notificationRead = QNotificationRead.notificationRead;
        QRoomMember roomMember = QRoomMember.roomMember;

        BooleanExpression notificationCondition = notification.user.id.eq(userId)
                .or(notification.type.eq(NotificationType.SYSTEM))
                .or(
                        notification.type.eq(NotificationType.ROOM)
                                .and(notification.room.id.in(
                                        JPAExpressions
                                                .select(roomMember.room.id)
                                                .from(roomMember)
                                                .where(roomMember.user.id.eq(userId))
                                ))
                );

        Long count = queryFactory
                .select(notification.count())
                .from(notification)
                .leftJoin(notificationRead)
                .on(notification.id.eq(notificationRead.notification.id)
                        .and(notificationRead.user.id.eq(userId)))
                .where(
                        notificationCondition,
                        notificationRead.id.isNull()
                )
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public Page<Notification> findUnreadByUserId(Long userId, Pageable pageable) {
        QNotification notification = QNotification.notification;
        QNotificationRead notificationRead = QNotificationRead.notificationRead;
        QRoomMember roomMember = QRoomMember.roomMember;

        BooleanExpression notificationCondition = notification.user.id.eq(userId)
                .or(notification.type.eq(NotificationType.SYSTEM))
                .or(
                        notification.type.eq(NotificationType.ROOM)
                                .and(notification.room.id.in(
                                        JPAExpressions
                                                .select(roomMember.room.id)
                                                .from(roomMember)
                                                .where(roomMember.user.id.eq(userId))
                                ))
                );

        List<Notification> content = queryFactory
                .selectFrom(notification)
                .leftJoin(notificationRead)
                .on(notification.id.eq(notificationRead.notification.id)
                        .and(notificationRead.user.id.eq(userId)))
                .where(
                        notificationCondition,
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
                        notificationCondition,
                        notificationRead.id.isNull()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
