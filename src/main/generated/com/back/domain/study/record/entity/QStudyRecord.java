package com.back.domain.study.record.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudyRecord is a Querydsl query type for StudyRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudyRecord extends EntityPathBase<StudyRecord> {

    private static final long serialVersionUID = -616987785L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudyRecord studyRecord = new QStudyRecord("studyRecord");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> duration = createNumber("duration", Long.class);

    public final DateTimePath<java.time.LocalDateTime> endTime = createDateTime("endTime", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<PauseInfo, QPauseInfo> pauseInfos = this.<PauseInfo, QPauseInfo>createList("pauseInfos", PauseInfo.class, QPauseInfo.class, PathInits.DIRECT2);

    public final com.back.domain.studyroom.entity.QRoom room;

    public final DateTimePath<java.time.LocalDateTime> startTime = createDateTime("startTime", java.time.LocalDateTime.class);

    public final com.back.domain.study.plan.entity.QStudyPlan studyPlan;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QStudyRecord(String variable) {
        this(StudyRecord.class, forVariable(variable), INITS);
    }

    public QStudyRecord(Path<? extends StudyRecord> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudyRecord(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudyRecord(PathMetadata metadata, PathInits inits) {
        this(StudyRecord.class, metadata, inits);
    }

    public QStudyRecord(Class<? extends StudyRecord> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new com.back.domain.studyroom.entity.QRoom(forProperty("room"), inits.get("room")) : null;
        this.studyPlan = inits.isInitialized("studyPlan") ? new com.back.domain.study.plan.entity.QStudyPlan(forProperty("studyPlan"), inits.get("studyPlan")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

