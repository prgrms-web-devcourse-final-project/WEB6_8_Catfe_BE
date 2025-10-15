package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomChatMessage is a Querydsl query type for RoomChatMessage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomChatMessage extends EntityPathBase<RoomChatMessage> {

    private static final long serialVersionUID = 901188035L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomChatMessage roomChatMessage = new QRoomChatMessage("roomChatMessage");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QRoom room;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QRoomChatMessage(String variable) {
        this(RoomChatMessage.class, forVariable(variable), INITS);
    }

    public QRoomChatMessage(Path<? extends RoomChatMessage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomChatMessage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomChatMessage(PathMetadata metadata, PathInits inits) {
        this(RoomChatMessage.class, metadata, inits);
    }

    public QRoomChatMessage(Class<? extends RoomChatMessage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new QRoom(forProperty("room"), inits.get("room")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

