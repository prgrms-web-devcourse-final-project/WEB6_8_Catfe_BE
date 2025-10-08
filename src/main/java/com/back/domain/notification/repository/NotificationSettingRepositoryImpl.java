package com.back.domain.notification.repository;

import com.back.domain.notification.entity.NotificationSetting;
import com.back.domain.notification.entity.NotificationSettingType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.back.domain.notification.entity.QNotificationSetting.notificationSetting;

@RequiredArgsConstructor
public class NotificationSettingRepositoryImpl implements NotificationSettingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<NotificationSetting> findAllByUserId(Long userId) {
        return queryFactory
                .selectFrom(notificationSetting)
                .where(notificationSetting.user.id.eq(userId))
                .fetch();
    }

    @Override
    public Optional<NotificationSetting> findByUserIdAndType(Long userId, NotificationSettingType type) {
        NotificationSetting result = queryFactory
                .selectFrom(notificationSetting)
                .where(
                        notificationSetting.user.id.eq(userId),
                        notificationSetting.type.eq(type)
                )
                .fetchOne(); // 결과가 없으면 null, 1개 초과면 NonUniqueResultException 발생

        return Optional.ofNullable(result);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        Integer fetchFirst = queryFactory
                .selectOne() // 존재 여부만 확인하므로 간단히 숫자 1을 조회
                .from(notificationSetting)
                .where(notificationSetting.user.id.eq(userId))
                .fetchFirst();

        return fetchFirst != null;
    }

    @Override
    public List<NotificationSetting> findEnabledSettingsByUserId(Long userId) {
        return queryFactory
                .selectFrom(notificationSetting)
                .where(
                        notificationSetting.user.id.eq(userId),
                        notificationSetting.enabled.isTrue()
                )
                .fetch();
    }
}