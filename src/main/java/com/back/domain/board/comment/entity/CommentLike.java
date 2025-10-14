package com.back.domain.board.comment.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"})
)
public class CommentLike extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // -------------------- 생성자 --------------------
    public CommentLike(Comment comment, User user) {
        this.comment = comment;
        this.user = user;
        comment.addLike(this);
        user.addCommentLike(this);
    }

    // -------------------- 헬퍼 메서드 --------------------
    /** 댓글 좋아요 삭제 시 연관관계 정리 */
    public void remove() {
        this.comment.removeLike(this);
        this.user.removeCommentLike(this);
    }
}
