package com.back.domain.board.comment.repository;

import com.back.domain.board.comment.entity.CommentLike;
import com.back.domain.board.comment.repository.custom.CommentLikeRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long>, CommentLikeRepositoryCustom {
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
}
