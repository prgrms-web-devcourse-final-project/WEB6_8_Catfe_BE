package com.back.domain.file.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFileAttachment is a Querydsl query type for FileAttachment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFileAttachment extends EntityPathBase<FileAttachment> {

    private static final long serialVersionUID = -1348427896L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFileAttachment fileAttachment = new QFileAttachment("fileAttachment");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final StringPath contentType = createString("contentType");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> fileSize = createNumber("fileSize", Long.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath originalName = createString("originalName");

    public final StringPath publicURL = createString("publicURL");

    public final StringPath storedName = createString("storedName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QFileAttachment(String variable) {
        this(FileAttachment.class, forVariable(variable), INITS);
    }

    public QFileAttachment(Path<? extends FileAttachment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFileAttachment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFileAttachment(PathMetadata metadata, PathInits inits) {
        this(FileAttachment.class, metadata, inits);
    }

    public QFileAttachment(Class<? extends FileAttachment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

