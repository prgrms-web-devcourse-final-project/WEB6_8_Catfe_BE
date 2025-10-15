package com.back.domain.file.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAttachmentMapping is a Querydsl query type for AttachmentMapping
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAttachmentMapping extends EntityPathBase<AttachmentMapping> {

    private static final long serialVersionUID = 70531522L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAttachmentMapping attachmentMapping = new QAttachmentMapping("attachmentMapping");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> entityId = createNumber("entityId", Long.class);

    public final EnumPath<EntityType> entityType = createEnum("entityType", EntityType.class);

    public final QFileAttachment fileAttachment;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QAttachmentMapping(String variable) {
        this(AttachmentMapping.class, forVariable(variable), INITS);
    }

    public QAttachmentMapping(Path<? extends AttachmentMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAttachmentMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAttachmentMapping(PathMetadata metadata, PathInits inits) {
        this(AttachmentMapping.class, metadata, inits);
    }

    public QAttachmentMapping(Class<? extends AttachmentMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.fileAttachment = inits.isInitialized("fileAttachment") ? new QFileAttachment(forProperty("fileAttachment"), inits.get("fileAttachment")) : null;
    }

}

