package com.back.domain.board.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPost is a Querydsl query type for Post
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPost extends EntityPathBase<Post> {

    private static final long serialVersionUID = -1354074411L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPost post = new QPost("post");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final NumberPath<Long> bookmarkCount = createNumber("bookmarkCount", Long.class);

    public final NumberPath<Long> commentCount = createNumber("commentCount", Long.class);

    public final ListPath<com.back.domain.board.comment.entity.Comment, com.back.domain.board.comment.entity.QComment> comments = this.<com.back.domain.board.comment.entity.Comment, com.back.domain.board.comment.entity.QComment>createList("comments", com.back.domain.board.comment.entity.Comment.class, com.back.domain.board.comment.entity.QComment.class, PathInits.DIRECT2);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Long> likeCount = createNumber("likeCount", Long.class);

    public final ListPath<PostBookmark, QPostBookmark> postBookmarks = this.<PostBookmark, QPostBookmark>createList("postBookmarks", PostBookmark.class, QPostBookmark.class, PathInits.DIRECT2);

    public final ListPath<PostCategoryMapping, QPostCategoryMapping> postCategoryMappings = this.<PostCategoryMapping, QPostCategoryMapping>createList("postCategoryMappings", PostCategoryMapping.class, QPostCategoryMapping.class, PathInits.DIRECT2);

    public final ListPath<PostLike, QPostLike> postLikes = this.<PostLike, QPostLike>createList("postLikes", PostLike.class, QPostLike.class, PathInits.DIRECT2);

    public final StringPath thumbnailUrl = createString("thumbnailUrl");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.back.domain.user.common.entity.QUser user;

    public QPost(String variable) {
        this(Post.class, forVariable(variable), INITS);
    }

    public QPost(Path<? extends Post> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPost(PathMetadata metadata, PathInits inits) {
        this(Post.class, metadata, inits);
    }

    public QPost(Class<? extends Post> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.back.domain.user.common.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

