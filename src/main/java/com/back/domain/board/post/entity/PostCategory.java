package com.back.domain.board.post.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
public class PostCategory extends BaseEntity {
    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCategoryMapping> postCategoryMappings;

    // -------------------- 생성자 --------------------
    public PostCategory(String name) {
        this.name = name;
        this.postCategoryMappings = new ArrayList<>();
    }
}