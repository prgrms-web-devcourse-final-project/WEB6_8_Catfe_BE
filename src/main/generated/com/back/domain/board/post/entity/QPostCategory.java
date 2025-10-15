package com.back.domain.board.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostCategory is a Querydsl query type for PostCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostCategory extends EntityPathBase<PostCategory> {

    private static final long serialVersionUID = -1270746381L;

    public static final QPostCategory postCategory = new QPostCategory("postCategory");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    public final ListPath<PostCategoryMapping, QPostCategoryMapping> postCategoryMappings = this.<PostCategoryMapping, QPostCategoryMapping>createList("postCategoryMappings", PostCategoryMapping.class, QPostCategoryMapping.class, PathInits.DIRECT2);

    public final EnumPath<com.back.domain.board.post.enums.CategoryType> type = createEnum("type", com.back.domain.board.post.enums.CategoryType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QPostCategory(String variable) {
        super(PostCategory.class, forVariable(variable));
    }

    public QPostCategory(Path<? extends PostCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPostCategory(PathMetadata metadata) {
        super(PostCategory.class, metadata);
    }

}

