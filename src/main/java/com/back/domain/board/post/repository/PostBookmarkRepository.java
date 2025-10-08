package com.back.domain.board.post.repository;

import com.back.domain.board.post.entity.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
}
