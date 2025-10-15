package com.back.domain.board.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostCategoryMapping is a Querydsl query type for PostCategoryMapping
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostCategoryMapping extends EntityPathBase<PostCategoryMapping> {

    private static final long serialVersionUID = 1022105179L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostCategoryMapping postCategoryMapping = new QPostCategoryMapping("postCategoryMapping");

    public final QPostCategory category;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QPost post;

    public QPostCategoryMapping(String variable) {
        this(PostCategoryMapping.class, forVariable(variable), INITS);
    }

    public QPostCategoryMapping(Path<? extends PostCategoryMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostCategoryMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostCategoryMapping(PathMetadata metadata, PathInits inits) {
        this(PostCategoryMapping.class, metadata, inits);
    }

    public QPostCategoryMapping(Class<? extends PostCategoryMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new QPostCategory(forProperty("category")) : null;
        this.post = inits.isInitialized("post") ? new QPost(forProperty("post"), inits.get("post")) : null;
    }

}

