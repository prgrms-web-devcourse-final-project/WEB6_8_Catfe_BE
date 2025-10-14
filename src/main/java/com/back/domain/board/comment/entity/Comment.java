package com.back.domain.board.comment.entity;

import com.back.domain.board.post.entity.Post;
import com.back.domain.user.common.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Comment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentLike> commentLikes = new ArrayList<>();

    // -------------------- 생성자 --------------------
    public Comment(Post post, User user, String content, Comment parent) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.parent = parent;
        post.addComment(this);
        user.addComment(this);
    }

    // -------------------- 정적 팩토리 메서드 --------------------
    /** 루트 댓글 생성 */
    public static Comment createRoot(Post post, User user, String content) {
        return new Comment(post, user, content, null);
    }

    /** 대댓글 생성 */
    public static Comment createChild(Post post, User user, String content, Comment parent) {
        Comment comment = new Comment(post, user, content, parent);
        parent.addChildren(comment);
        return comment;
    }

    // -------------------- 연관관계 편의 메서드 --------------------
    public void addLike(CommentLike like) {
        this.commentLikes.add(like);
    }

    public void removeLike(CommentLike like) {
        this.commentLikes.remove(like);
    }

    public void addChildren(Comment child) {
        this.children.add(child);
    }

    public void removeChildren(Comment child) {
        this.children.remove(child);
    }

    // -------------------- 비즈니스 메서드 --------------------
    /** 댓글 내용 수정 */
    public void update(String content) {
        this.content = content;
    }

    /** 좋아요 수 증가 */
    public void increaseLikeCount() {
        this.likeCount++;
    }

    /** 좋아요 수 감소 */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // -------------------- 헬퍼 메서드 --------------------
    /** 댓글 삭제 시 연관관계 정리 */
    public void remove() {
        this.post.removeComment(this);
        this.user.removeComment(this);
        if (this.parent != null) {
            this.parent.removeChildren(this);
        }
    }
}
