package com.back.domain.board.post.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "category_id"})
)
public class PostCategoryMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PostCategory category;

    // -------------------- 생성자 --------------------
    public PostCategoryMapping(Post post, PostCategory category) {
        this.post = post;
        this.category = category;
        post.addPostCategoryMapping(this);
        category.addPostCategoryMapping(this);
    }

    // -------------------- 헬퍼 메서드 --------------------
    /** 매핑 삭제 시 연관관계 정리 */
    public void remove() {
        this.post.removePostCategoryMapping(this);
        this.category.removePostCategoryMapping(this);
    }
}
