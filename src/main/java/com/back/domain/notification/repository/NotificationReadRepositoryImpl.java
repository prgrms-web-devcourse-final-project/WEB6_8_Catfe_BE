package com.back.domain.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.back.domain.notification.entity.QNotificationRead.notificationRead;

@RequiredArgsConstructor
public class NotificationReadRepositoryImpl implements NotificationReadRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Set<Long> findReadNotificationIds(Long userId, List<Long> notificationIds) {

        if (notificationIds == null || notificationIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Long> readIds = queryFactory
                .select(notificationRead.notification.id)
                .from(notificationRead)
                .where(
                        notificationRead.user.id.eq(userId),
                        notificationRead.notification.id.in(notificationIds)
                )
                .fetch();

        return new HashSet<>(readIds);
    }
}