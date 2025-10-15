package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomInviteCode is a Querydsl query type for RoomInviteCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomInviteCode extends EntityPathBase<RoomInviteCode> {

    private static final long serialVersionUID = -1777216030L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomInviteCode roomInviteCode = new QRoomInviteCode("roomInviteCode");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.back.domain.user.common.entity.QUser createdBy;

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath inviteCode = createString("inviteCode");

    public final BooleanPath isActive = createBoolean("isActive");

    public final QRoom room;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRoomInviteCode(String variable) {
        this(RoomInviteCode.class, forVariable(variable), INITS);
    }

    public QRoomInviteCode(Path<? extends RoomInviteCode> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomInviteCode(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomInviteCode(PathMetadata metadata, PathInits inits) {
        this(RoomInviteCode.class, metadata, inits);
    }

    public QRoomInviteCode(Class<? extends RoomInviteCode> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.createdBy = inits.isInitialized("createdBy") ? new com.back.domain.user.common.entity.QUser(forProperty("createdBy"), inits.get("createdBy")) : null;
        this.room = inits.isInitialized("room") ? new QRoom(forProperty("room"), inits.get("room")) : null;
    }

}

