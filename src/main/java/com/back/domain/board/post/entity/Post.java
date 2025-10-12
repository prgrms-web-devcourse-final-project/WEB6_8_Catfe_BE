package com.back.domain.board.post.entity;

import com.back.domain.board.comment.entity.Comment;
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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column(nullable = false)
    private Long bookmarkCount = 0L;

    @Column(nullable = false)
    private Long commentCount = 0L;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCategoryMapping> postCategoryMappings = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostBookmark> postBookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // -------------------- 생성자 --------------------
    public Post(User user, String title, String content, String thumbnailUrl) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
        user.addPost(this);
    }

    // -------------------- 연관관계 편의 메서드 --------------------
    public void addPostCategoryMapping(PostCategoryMapping mapping) {
        this.postCategoryMappings.add(mapping);
    }

    public void addLike(PostLike like) {
        this.postLikes.add(like);
    }

    public void removeLike(PostLike like) {
        this.postLikes.remove(like);
    }

    public void addBookmark(PostBookmark bookmark) {
        this.postBookmarks.add(bookmark);
    }

    public void removeBookmark(PostBookmark bookmark) {
        this.postBookmarks.remove(bookmark);
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
    }

    public void removeComment(Comment comment) {
        this.comments.remove(comment);
    }

    // -------------------- 비즈니스 메서드 --------------------
    /** 게시글 내용 수정 */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // TODO: 진짜로 바뀐 카테고리만 추가/삭제하도록 개선
    /** 카테고리 일괄 업데이트 */
    public void updateCategories(List<PostCategory> categories) {
        this.postCategoryMappings.clear();
        categories.forEach(category -> new PostCategoryMapping(this, category));
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

    /** 북마크 수 증가 */
    public void increaseBookmarkCount() {
        this.bookmarkCount++;
    }

    /** 북마크 수 감소 */
    public void decreaseBookmarkCount() {
        if (this.bookmarkCount > 0) {
            this.bookmarkCount--;
        }
    }

    /** 댓글 수 증가 */
    public void increaseCommentCount() {
        this.commentCount++;
    }

    /** 댓글 수 감소 */
    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    // -------------------- 헬퍼 메서드 --------------------
    /** 게시글에 연결된 카테고리 목록 조회 */
    public List<PostCategory> getCategories() {
        return postCategoryMappings.stream()
                .map(PostCategoryMapping::getCategory)
                .toList();
    }
}
