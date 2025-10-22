package com.back.domain.board.post.repository;

import com.back.domain.board.post.entity.PostCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostCategoryMappingRepository extends JpaRepository<PostCategoryMapping, Long> {
}
