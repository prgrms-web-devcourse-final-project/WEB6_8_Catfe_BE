package com.back.domain.study.record.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPauseInfo is a Querydsl query type for PauseInfo
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPauseInfo extends EntityPathBase<PauseInfo> {

    private static final long serialVersionUID = -1926805759L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPauseInfo pauseInfo = new QPauseInfo("pauseInfo");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> pausedAt = createDateTime("pausedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> restartAt = createDateTime("restartAt", java.time.LocalDateTime.class);

    public final QStudyRecord studyRecord;

    public QPauseInfo(String variable) {
        this(PauseInfo.class, forVariable(variable), INITS);
    }

    public QPauseInfo(Path<? extends PauseInfo> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPauseInfo(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPauseInfo(PathMetadata metadata, PathInits inits) {
        this(PauseInfo.class, metadata, inits);
    }

    public QPauseInfo(Class<? extends PauseInfo> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.studyRecord = inits.isInitialized("studyRecord") ? new QStudyRecord(forProperty("studyRecord"), inits.get("studyRecord")) : null;
    }

}

