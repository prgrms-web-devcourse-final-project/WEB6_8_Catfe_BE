package com.back.domain.board.post.repository;

import com.back.domain.board.post.entity.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {
    boolean existsByName(String name);
    List<PostCategory> findAllByNameIn(List<String> categoryNames);
}
