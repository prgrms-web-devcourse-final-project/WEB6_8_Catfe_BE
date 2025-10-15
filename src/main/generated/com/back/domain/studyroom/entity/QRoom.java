package com.back.domain.studyroom.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoom is a Querydsl query type for Room
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoom extends EntityPathBase<Room> {

    private static final long serialVersionUID = -1507681236L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoom room = new QRoom("room");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final BooleanPath allowAudio = createBoolean("allowAudio");

    public final BooleanPath allowCamera = createBoolean("allowCamera");

    public final BooleanPath allowScreenShare = createBoolean("allowScreenShare");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.back.domain.user.common.entity.QUser createdBy;

    public final StringPath description = createString("description");

    public final ListPath<RoomGuestbook, QRoomGuestbook> guestbooks = this.<RoomGuestbook, QRoomGuestbook>createList("guestbooks", RoomGuestbook.class, QRoomGuestbook.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isActive = createBoolean("isActive");

    public final BooleanPath isPrivate = createBoolean("isPrivate");

    public final NumberPath<Integer> maxParticipants = createNumber("maxParticipants", Integer.class);

    public final StringPath password = createString("password");

    public final ListPath<RoomChatMessage, QRoomChatMessage> roomChatMessages = this.<RoomChatMessage, QRoomChatMessage>createList("roomChatMessages", RoomChatMessage.class, QRoomChatMessage.class, PathInits.DIRECT2);

    public final ListPath<RoomMember, QRoomMember> roomMembers = this.<RoomMember, QRoomMember>createList("roomMembers", RoomMember.class, QRoomMember.class, PathInits.DIRECT2);

    public final ListPath<RoomParticipantHistory, QRoomParticipantHistory> roomParticipantHistories = this.<RoomParticipantHistory, QRoomParticipantHistory>createList("roomParticipantHistories", RoomParticipantHistory.class, QRoomParticipantHistory.class, PathInits.DIRECT2);

    public final EnumPath<RoomStatus> status = createEnum("status", RoomStatus.class);

    public final ListPath<com.back.domain.study.record.entity.StudyRecord, com.back.domain.study.record.entity.QStudyRecord> studyRecords = this.<com.back.domain.study.record.entity.StudyRecord, com.back.domain.study.record.entity.QStudyRecord>createList("studyRecords", com.back.domain.study.record.entity.StudyRecord.class, com.back.domain.study.record.entity.QStudyRecord.class, PathInits.DIRECT2);

    public final QRoomTheme theme;

    public final StringPath thumbnailUrl = createString("thumbnailUrl");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRoom(String variable) {
        this(Room.class, forVariable(variable), INITS);
    }

    public QRoom(Path<? extends Room> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoom(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoom(PathMetadata metadata, PathInits inits) {
        this(Room.class, metadata, inits);
    }

    public QRoom(Class<? extends Room> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.createdBy = inits.isInitialized("createdBy") ? new com.back.domain.user.common.entity.QUser(forProperty("createdBy"), inits.get("createdBy")) : null;
        this.theme = inits.isInitialized("theme") ? new QRoomTheme(forProperty("theme")) : null;
    }

}

