package com.back.domain.board.comment.repository;

import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.repository.custom.CommentRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {
    Page<Comment> findAllByUserId(Long userId, Pageable pageable);
}
