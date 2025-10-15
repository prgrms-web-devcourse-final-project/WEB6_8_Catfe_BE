package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomParticipantHistory is a Querydsl query type for RoomParticipantHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomParticipantHistory extends EntityPathBase<RoomParticipantHistory> {

    private static final long serialVersionUID = -1793342675L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomParticipantHistory roomParticipantHistory = new QRoomParticipantHistory("roomParticipantHistory");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> leftAt = createDateTime("leftAt", java.time.LocalDateTime.class);

    public final QRoom room;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QRoomParticipantHistory(String variable) {
        this(RoomParticipantHistory.class, forVariable(variable), INITS);
    }

    public QRoomParticipantHistory(Path<? extends RoomParticipantHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomParticipantHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomParticipantHistory(PathMetadata metadata, PathInits inits) {
        this(RoomParticipantHistory.class, metadata, inits);
    }

    public QRoomParticipantHistory(Class<? extends RoomParticipantHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new QRoom(forProperty("room"), inits.get("room")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

