package com.back.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotificationRead is a Querydsl query type for NotificationRead
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationRead extends EntityPathBase<NotificationRead> {

    private static final long serialVersionUID = -150077159L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotificationRead notificationRead = new QNotificationRead("notificationRead");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QNotification notification;

    public final DateTimePath<java.time.LocalDateTime> readAt = createDateTime("readAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QNotificationRead(String variable) {
        this(NotificationRead.class, forVariable(variable), INITS);
    }

    public QNotificationRead(Path<? extends NotificationRead> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotificationRead(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotificationRead(PathMetadata metadata, PathInits inits) {
        this(NotificationRead.class, metadata, inits);
    }

    public QNotificationRead(Class<? extends NotificationRead> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.notification = inits.isInitialized("notification") ? new QNotification(forProperty("notification"), inits.get("notification")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

