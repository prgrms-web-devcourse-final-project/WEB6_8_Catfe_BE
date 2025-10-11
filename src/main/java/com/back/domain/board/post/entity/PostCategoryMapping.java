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
}
