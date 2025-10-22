package com.back.domain.board.post.repository;

import com.back.domain.board.post.entity.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCategoryRepository extends JpaRepository<PostCategory, Long> {
    boolean existsByName(String name);

    @Query("SELECT c.id FROM PostCategory c WHERE c.name IN :names")
    List<Long> findIdsByNameIn(@Param("names") List<String> names);
}
