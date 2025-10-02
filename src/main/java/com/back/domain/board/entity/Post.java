package com.back.domain.board.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Post extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;

    private String content;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCategoryMapping> postCategoryMappings = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostBookmark> postBookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // -------------------- 생성자 --------------------
    public Post(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
    }

    // -------------------- 비즈니스 메서드 --------------------
    // 게시글 업데이트
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // 카테고리 업데이트
    public void updateCategories(List<PostCategory> categories) {
        this.postCategoryMappings.clear();
        categories.forEach(category ->
                this.postCategoryMappings.add(new PostCategoryMapping(this, category))
        );
    }

    // -------------------- 헬퍼 메서드 --------------------
    // 게시글에 연결된 카테고리 목록 조회
    public List<PostCategory> getCategories() {
        return postCategoryMappings.stream()
                .map(PostCategoryMapping::getCategory)
                .toList();
    }
}
