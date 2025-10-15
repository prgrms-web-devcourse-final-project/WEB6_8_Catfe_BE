package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomMember is a Querydsl query type for RoomMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomMember extends EntityPathBase<RoomMember> {

    private static final long serialVersionUID = -243842586L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomMember roomMember = new QRoomMember("roomMember");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.time.LocalDateTime> joinedAt = createDateTime("joinedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> promotedAt = createDateTime("promotedAt", java.time.LocalDateTime.class);

    public final EnumPath<RoomRole> role = createEnum("role", RoomRole.class);

    public final QRoom room;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QRoomMember(String variable) {
        this(RoomMember.class, forVariable(variable), INITS);
    }

    public QRoomMember(Path<? extends RoomMember> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomMember(PathMetadata metadata, PathInits inits) {
        this(RoomMember.class, metadata, inits);
    }

    public QRoomMember(Class<? extends RoomMember> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new QRoom(forProperty("room"), inits.get("room")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

