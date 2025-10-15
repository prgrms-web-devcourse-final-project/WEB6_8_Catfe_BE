package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomAnnouncement is a Querydsl query type for RoomAnnouncement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomAnnouncement extends EntityPathBase<RoomAnnouncement> {

    private static final long serialVersionUID = 1824611219L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomAnnouncement roomAnnouncement = new QRoomAnnouncement("roomAnnouncement");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.back.domain.user.common.entity.QUser createdBy;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isPinned = createBoolean("isPinned");

    public final DateTimePath<java.time.LocalDateTime> pinnedAt = createDateTime("pinnedAt", java.time.LocalDateTime.class);

    public final QRoom room;

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRoomAnnouncement(String variable) {
        this(RoomAnnouncement.class, forVariable(variable), INITS);
    }

    public QRoomAnnouncement(Path<? extends RoomAnnouncement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomAnnouncement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomAnnouncement(PathMetadata metadata, PathInits inits) {
        this(RoomAnnouncement.class, metadata, inits);
    }

    public QRoomAnnouncement(Class<? extends RoomAnnouncement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.createdBy = inits.isInitialized("createdBy") ? new com.back.domain.user.common.entity.QUser(forProperty("createdBy"), inits.get("createdBy")) : null;
        this.room = inits.isInitialized("room") ? new QRoom(forProperty("room"), inits.get("room")) : null;
    }

}

