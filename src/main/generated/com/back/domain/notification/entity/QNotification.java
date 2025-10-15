package com.back.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotification is a Querydsl query type for Notification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotification extends EntityPathBase<Notification> {

    private static final long serialVersionUID = 1156996323L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotification notification = new QNotification("notification");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final com.back.domain.user.common.entity.QUser actor;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<NotificationRead, QNotificationRead> notificationReads = this.<NotificationRead, QNotificationRead>createList("notificationReads", NotificationRead.class, QNotificationRead.class, PathInits.DIRECT2);

    public final com.back.domain.user.common.entity.QUser receiver;

    public final com.back.domain.studyroom.entity.QRoom room;

    public final StringPath targetUrl = createString("targetUrl");

    public final StringPath title = createString("title");

    public final EnumPath<NotificationType> type = createEnum("type", NotificationType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNotification(String variable) {
        this(Notification.class, forVariable(variable), INITS);
    }

    public QNotification(Path<? extends Notification> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotification(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotification(PathMetadata metadata, PathInits inits) {
        this(Notification.class, metadata, inits);
    }

    public QNotification(Class<? extends Notification> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.actor = inits.isInitialized("actor") ? new com.back.domain.user.common.entity.QUser(forProperty("actor"), inits.get("actor")) : null;
        this.receiver = inits.isInitialized("receiver") ? new com.back.domain.user.common.entity.QUser(forProperty("receiver"), inits.get("receiver")) : null;
        this.room = inits.isInitialized("room") ? new com.back.domain.studyroom.entity.QRoom(forProperty("room"), inits.get("room")) : null;
    }

}

