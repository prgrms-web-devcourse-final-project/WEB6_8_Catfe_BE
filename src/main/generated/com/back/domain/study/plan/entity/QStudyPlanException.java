package com.back.domain.study.plan.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStudyPlanException is a Querydsl query type for StudyPlanException
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStudyPlanException extends EntityPathBase<StudyPlanException> {

    private static final long serialVersionUID = -687734952L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStudyPlanException studyPlanException = new QStudyPlanException("studyPlanException");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final EnumPath<ApplyScope> applyScope = createEnum("applyScope", ApplyScope.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DatePath<java.time.LocalDate> exceptionDate = createDate("exceptionDate", java.time.LocalDate.class);

    public final EnumPath<StudyPlanException.ExceptionType> exceptionType = createEnum("exceptionType", StudyPlanException.ExceptionType.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<Color> modifiedColor = createEnum("modifiedColor", Color.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedEndDate = createDateTime("modifiedEndDate", java.time.LocalDateTime.class);

    public final QRepeatRuleEmbeddable modifiedRepeatRule;

    public final DateTimePath<java.time.LocalDateTime> modifiedStartDate = createDateTime("modifiedStartDate", java.time.LocalDateTime.class);

    public final StringPath modifiedSubject = createString("modifiedSubject");

    public final QStudyPlan studyPlan;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QStudyPlanException(String variable) {
        this(StudyPlanException.class, forVariable(variable), INITS);
    }

    public QStudyPlanException(Path<? extends StudyPlanException> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStudyPlanException(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStudyPlanException(PathMetadata metadata, PathInits inits) {
        this(StudyPlanException.class, metadata, inits);
    }

    public QStudyPlanException(Class<? extends StudyPlanException> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.modifiedRepeatRule = inits.isInitialized("modifiedRepeatRule") ? new QRepeatRuleEmbeddable(forProperty("modifiedRepeatRule")) : null;
        this.studyPlan = inits.isInitialized("studyPlan") ? new QStudyPlan(forProperty("studyPlan"), inits.get("studyPlan")) : null;
    }

}

