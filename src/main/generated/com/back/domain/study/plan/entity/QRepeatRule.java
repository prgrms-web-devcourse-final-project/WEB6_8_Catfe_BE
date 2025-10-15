package com.back.domain.study.plan.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRepeatRule is a Querydsl query type for RepeatRule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRepeatRule extends EntityPathBase<RepeatRule> {

    private static final long serialVersionUID = -1656608334L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRepeatRule repeatRule = new QRepeatRule("repeatRule");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final ListPath<DayOfWeek, EnumPath<DayOfWeek>> byDay = this.<DayOfWeek, EnumPath<DayOfWeek>>createList("byDay", DayOfWeek.class, EnumPath.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<Frequency> frequency = createEnum("frequency", Frequency.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> repeatInterval = createNumber("repeatInterval", Integer.class);

    public final QStudyPlan studyPlan;

    public final DatePath<java.time.LocalDate> untilDate = createDate("untilDate", java.time.LocalDate.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QRepeatRule(String variable) {
        this(RepeatRule.class, forVariable(variable), INITS);
    }

    public QRepeatRule(Path<? extends RepeatRule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRepeatRule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRepeatRule(PathMetadata metadata, PathInits inits) {
        this(RepeatRule.class, metadata, inits);
    }

    public QRepeatRule(Class<? extends RepeatRule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.studyPlan = inits.isInitialized("studyPlan") ? new QStudyPlan(forProperty("studyPlan"), inits.get("studyPlan")) : null;
    }

}

