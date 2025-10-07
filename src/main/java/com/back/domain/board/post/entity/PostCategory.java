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
public class PostCategory extends BaseEntity {
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCategoryMapping> postCategoryMappings;

    // -------------------- 생성자 --------------------
    public PostCategory(String name) {
        this.name = name;
        this.type = CategoryType.SUBJECT;
        this.postCategoryMappings = new ArrayList<>();
    }

    public PostCategory(String name, CategoryType type) {
        this.name = name;
        this.type = type;
        this.postCategoryMappings = new ArrayList<>();
    }
}