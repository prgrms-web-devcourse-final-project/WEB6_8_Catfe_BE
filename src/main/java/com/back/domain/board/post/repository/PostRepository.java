package com.back.domain.board.post.repository;

import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.repository.custom.PostRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
}
