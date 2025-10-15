package com.back.domain.study.plan.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRepeatRuleEmbeddable is a Querydsl query type for RepeatRuleEmbeddable
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QRepeatRuleEmbeddable extends BeanPath<RepeatRuleEmbeddable> {

    private static final long serialVersionUID = -424549417L;

    public static final QRepeatRuleEmbeddable repeatRuleEmbeddable = new QRepeatRuleEmbeddable("repeatRuleEmbeddable");

    public final ListPath<DayOfWeek, EnumPath<DayOfWeek>> byDay = this.<DayOfWeek, EnumPath<DayOfWeek>>createList("byDay", DayOfWeek.class, EnumPath.class, PathInits.DIRECT2);

    public final EnumPath<Frequency> frequency = createEnum("frequency", Frequency.class);

    public final NumberPath<Integer> repeatInterval = createNumber("repeatInterval", Integer.class);

    public final DatePath<java.time.LocalDate> untilDate = createDate("untilDate", java.time.LocalDate.class);

    public QRepeatRuleEmbeddable(String variable) {
        super(RepeatRuleEmbeddable.class, forVariable(variable));
    }

    public QRepeatRuleEmbeddable(Path<? extends RepeatRuleEmbeddable> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRepeatRuleEmbeddable(PathMetadata metadata) {
        super(RepeatRuleEmbeddable.class, metadata);
    }

}

