package com.back.domain.user.common.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 567479142L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final com.back.global.entity.QBaseEntity _super = new com.back.global.entity.QBaseEntity(this);

    public final ListPath<com.back.domain.board.comment.entity.CommentLike, com.back.domain.board.comment.entity.QCommentLike> commentLikes = this.<com.back.domain.board.comment.entity.CommentLike, com.back.domain.board.comment.entity.QCommentLike>createList("commentLikes", com.back.domain.board.comment.entity.CommentLike.class, com.back.domain.board.comment.entity.QCommentLike.class, PathInits.DIRECT2);

    public final ListPath<com.back.domain.board.comment.entity.Comment, com.back.domain.board.comment.entity.QComment> comments = this.<com.back.domain.board.comment.entity.Comment, com.back.domain.board.comment.entity.QComment>createList("comments", com.back.domain.board.comment.entity.Comment.class, com.back.domain.board.comment.entity.QComment.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final ListPath<com.back.domain.file.entity.FileAttachment, com.back.domain.file.entity.QFileAttachment> fileAttachments = this.<com.back.domain.file.entity.FileAttachment, com.back.domain.file.entity.QFileAttachment>createList("fileAttachments", com.back.domain.file.entity.FileAttachment.class, com.back.domain.file.entity.QFileAttachment.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath password = createString("password");

    public final ListPath<com.back.domain.board.post.entity.PostBookmark, com.back.domain.board.post.entity.QPostBookmark> postBookmarks = this.<com.back.domain.board.post.entity.PostBookmark, com.back.domain.board.post.entity.QPostBookmark>createList("postBookmarks", com.back.domain.board.post.entity.PostBookmark.class, com.back.domain.board.post.entity.QPostBookmark.class, PathInits.DIRECT2);

    public final ListPath<com.back.domain.board.post.entity.PostLike, com.back.domain.board.post.entity.QPostLike> postLikes = this.<com.back.domain.board.post.entity.PostLike, com.back.domain.board.post.entity.QPostLike>createList("postLikes", com.back.domain.board.post.entity.PostLike.class, com.back.domain.board.post.entity.QPostLike.class, PathInits.DIRECT2);

    public final ListPath<com.back.domain.board.post.entity.Post, com.back.domain.board.post.entity.QPost> posts = this.<com.back.domain.board.post.entity.Post, com.back.domain.board.post.entity.QPost>createList("posts", com.back.domain.board.post.entity.Post.class, com.back.domain.board.post.entity.QPost.class, PathInits.DIRECT2);

    public final StringPath provider = createString("provider");

    public final StringPath providerId = createString("providerId");

    public final EnumPath<com.back.domain.user.common.enums.Role> role = createEnum("role", com.back.domain.user.common.enums.Role.class);

    public final ListPath<com.back.domain.studyroom.entity.RoomChatMessage, com.back.domain.studyroom.entity.QRoomChatMessage> roomChatMessages = this.<com.back.domain.studyroom.entity.RoomChatMessage, com.back.domain.studyroom.entity.QRoomChatMessage>createList("roomChatMessages", com.back.domain.studyroom.entity.RoomChatMessage.class, com.back.domain.studyroom.entity.QRoomChatMessage.class, PathInits.DIRECT2);

    public final ListPath<com.back.domain.studyroom.entity.RoomMember, com.back.domain.studyroom.entity.QRoomMember> roomMembers = this.<com.back.domain.studyroom.entity.RoomMember, com.back.domain.studyroom.entity.QRoomMember>createList("roomMembers", com.back.domain.studyroom.entity.RoomMember.class, com.back.domain.studyroom.entity.QRoomMember.class, PathInits.DIRECT2);

    public final ListPath<com.back.domain.studyroom.entity.RoomParticipantHistory, com.back.domain.studyroom.entity.QRoomParticipantHistory> roomParticipantHistories = this.<com.back.domain.studyroom.entity.RoomParticipantHistory, com.back.domain.studyroom.entity.QRoomParticipantHistory>createList("roomParticipantHistories", com.back.domain.studyroom.entity.RoomParticipantHistory.class, com.back.domain.studyroom.entity.QRoomParticipantHistory.class, PathInits.DIRECT2);

    public final ListPath<com.back.domain.study.plan.entity.StudyPlan, com.back.domain.study.plan.entity.QStudyPlan> studyPlans = this.<com.back.domain.study.plan.entity.StudyPlan, com.back.domain.study.plan.entity.QStudyPlan>createList("studyPlans", com.back.domain.study.plan.entity.StudyPlan.class, com.back.domain.study.plan.entity.QStudyPlan.class, PathInits.DIRECT2);

    public final ListPath<com.back.domain.study.todo.entity.Todo, com.back.domain.study.todo.entity.QTodo> todos = this.<com.back.domain.study.todo.entity.Todo, com.back.domain.study.todo.entity.QTodo>createList("todos", com.back.domain.study.todo.entity.Todo.class, com.back.domain.study.todo.entity.QTodo.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath username = createString("username");

    public final QUserProfile userProfile;

    public final EnumPath<com.back.domain.user.common.enums.UserStatus> userStatus = createEnum("userStatus", com.back.domain.user.common.enums.UserStatus.class);

    public final ListPath<UserToken, QUserToken> userTokens = this.<UserToken, QUserToken>createList("userTokens", UserToken.class, QUserToken.class, PathInits.DIRECT2);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.userProfile = inits.isInitialized("userProfile") ? new QUserProfile(forProperty("userProfile"), inits.get("userProfile")) : null;
    }

}

