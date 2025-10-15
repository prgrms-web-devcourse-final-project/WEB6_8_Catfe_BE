package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomGuestbook is a Querydsl query type for RoomGuestbook
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomGuestbook extends EntityPathBase<RoomGuestbook> {

    private static final long serialVersionUID = 540291573L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomGuestbook roomGuestbook = new QRoomGuestbook("roomGuestbook");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<RoomGuestbookReaction, QRoomGuestbookReaction> reactions = this.<RoomGuestbookReaction, QRoomGuestbookReaction>createList("reactions", RoomGuestbookReaction.class, QRoomGuestbookReaction.class, PathInits.DIRECT2);

    public final QRoom room;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QRoomGuestbook(String variable) {
        this(RoomGuestbook.class, forVariable(variable), INITS);
    }

    public QRoomGuestbook(Path<? extends RoomGuestbook> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomGuestbook(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomGuestbook(PathMetadata metadata, PathInits inits) {
        this(RoomGuestbook.class, metadata, inits);
    }

    public QRoomGuestbook(Class<? extends RoomGuestbook> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new QRoom(forProperty("room"), inits.get("room")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

