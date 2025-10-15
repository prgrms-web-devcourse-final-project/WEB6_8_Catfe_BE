package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomGuestbookPin is a Querydsl query type for RoomGuestbookPin
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomGuestbookPin extends EntityPathBase<RoomGuestbookPin> {

    private static final long serialVersionUID = -1711093920L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomGuestbookPin roomGuestbookPin = new QRoomGuestbookPin("roomGuestbookPin");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QRoomGuestbook guestbook;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QRoomGuestbookPin(String variable) {
        this(RoomGuestbookPin.class, forVariable(variable), INITS);
    }

    public QRoomGuestbookPin(Path<? extends RoomGuestbookPin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomGuestbookPin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomGuestbookPin(PathMetadata metadata, PathInits inits) {
        this(RoomGuestbookPin.class, metadata, inits);
    }

    public QRoomGuestbookPin(Class<? extends RoomGuestbookPin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.guestbook = inits.isInitialized("guestbook") ? new QRoomGuestbook(forProperty("guestbook"), inits.get("guestbook")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

