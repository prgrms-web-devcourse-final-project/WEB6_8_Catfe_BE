package com.back.domain.board.post.entity;

import com.back.domain.board.post.enums.CategoryType;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Table(
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "type"})}
)
public class PostCategory extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCategoryMapping> postCategoryMappings = new ArrayList<>();

    // -------------------- 생성자 --------------------
    public PostCategory(String name, CategoryType type) {
        this.name = name;
        this.type = type;
        this.postCategoryMappings = new ArrayList<>();
    }

    // -------------------- 연관관계 편의 메서드 --------------------
    public void addPostCategoryMapping(PostCategoryMapping mapping) {
        this.postCategoryMappings.add(mapping);
    }

    public void removePostCategoryMapping(PostCategoryMapping mapping) {
        this.postCategoryMappings.remove(mapping);
    }
}