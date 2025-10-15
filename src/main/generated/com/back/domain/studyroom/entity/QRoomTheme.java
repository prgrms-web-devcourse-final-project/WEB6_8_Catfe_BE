package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomTheme is a Querydsl query type for RoomTheme
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomTheme extends EntityPathBase<RoomTheme> {

    private static final long serialVersionUID = 1661248765L;

    public static final QRoomTheme roomTheme = new QRoomTheme("roomTheme");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath name = createString("name");

    public final ListPath<Room, QRoom> rooms = this.<Room, QRoom>createList("rooms", Room.class, QRoom.class, PathInits.DIRECT2);

    public final EnumPath<RoomType> type = createEnum("type", RoomType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRoomTheme(String variable) {
        super(RoomTheme.class, forVariable(variable));
    }

    public QRoomTheme(Path<? extends RoomTheme> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRoomTheme(PathMetadata metadata) {
        super(RoomTheme.class, metadata);
    }

}

