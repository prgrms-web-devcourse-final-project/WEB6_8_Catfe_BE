package com.back.domain.study.plan.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudyPlan is a Querydsl query type for StudyPlan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudyPlan extends EntityPathBase<StudyPlan> {

    private static final long serialVersionUID = -522219849L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudyPlan studyPlan = new QStudyPlan("studyPlan");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final EnumPath<Color> color = createEnum("color", Color.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> endDate = createDateTime("endDate", java.time.LocalDateTime.class);

    public final ListPath<StudyPlanException, QStudyPlanException> exceptions = this.<StudyPlanException, QStudyPlanException>createList("exceptions", StudyPlanException.class, QStudyPlanException.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QRepeatRule repeatRule;

    public final DateTimePath<java.time.LocalDateTime> startDate = createDateTime("startDate", java.time.LocalDateTime.class);

    public final ListPath<com.back.domain.study.record.entity.StudyRecord, com.back.domain.study.record.entity.QStudyRecord> studyRecords = this.<com.back.domain.study.record.entity.StudyRecord, com.back.domain.study.record.entity.QStudyRecord>createList("studyRecords", com.back.domain.study.record.entity.StudyRecord.class, com.back.domain.study.record.entity.QStudyRecord.class, PathInits.DIRECT2);

    public final StringPath subject = createString("subject");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QStudyPlan(String variable) {
        this(StudyPlan.class, forVariable(variable), INITS);
    }

    public QStudyPlan(Path<? extends StudyPlan> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudyPlan(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudyPlan(PathMetadata metadata, PathInits inits) {
        this(StudyPlan.class, metadata, inits);
    }

    public QStudyPlan(Class<? extends StudyPlan> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.repeatRule = inits.isInitialized("repeatRule") ? new QRepeatRule(forProperty("repeatRule"), inits.get("repeatRule")) : null;
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

