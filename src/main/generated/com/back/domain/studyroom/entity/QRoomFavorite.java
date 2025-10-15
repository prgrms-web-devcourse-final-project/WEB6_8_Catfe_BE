package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomFavorite is a Querydsl query type for RoomFavorite
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomFavorite extends EntityPathBase<RoomFavorite> {

    private static final long serialVersionUID = -704258584L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomFavorite roomFavorite = new QRoomFavorite("roomFavorite");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QRoom room;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QRoomFavorite(String variable) {
        this(RoomFavorite.class, forVariable(variable), INITS);
    }

    public QRoomFavorite(Path<? extends RoomFavorite> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomFavorite(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomFavorite(PathMetadata metadata, PathInits inits) {
        this(RoomFavorite.class, metadata, inits);
    }

    public QRoomFavorite(Class<? extends RoomFavorite> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new QRoom(forProperty("room"), inits.get("room")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

