package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomGuestbookReaction is a Querydsl query type for RoomGuestbookReaction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomGuestbookReaction extends EntityPathBase<RoomGuestbookReaction> {

    private static final long serialVersionUID = 2065083198L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomGuestbookReaction roomGuestbookReaction = new QRoomGuestbookReaction("roomGuestbookReaction");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath emoji = createString("emoji");

    public final QRoomGuestbook guestbook;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QRoomGuestbookReaction(String variable) {
        this(RoomGuestbookReaction.class, forVariable(variable), INITS);
    }

    public QRoomGuestbookReaction(Path<? extends RoomGuestbookReaction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomGuestbookReaction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomGuestbookReaction(PathMetadata metadata, PathInits inits) {
        this(RoomGuestbookReaction.class, metadata, inits);
    }

    public QRoomGuestbookReaction(Class<? extends RoomGuestbookReaction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.guestbook = inits.isInitialized("guestbook") ? new QRoomGuestbook(forProperty("guestbook"), inits.get("guestbook")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

