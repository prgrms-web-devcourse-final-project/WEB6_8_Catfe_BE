package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvatarRepository extends JpaRepository<Avatar, Long> {
    
    /**
     * 정렬 순서대로 모든 아바타 조회
     */
    List<Avatar> findAllByOrderBySortOrderAsc();
    
    /**
     * 기본 아바타만 조회 (랜덤 배정용)
     */
    List<Avatar> findByIsDefaultTrueOrderBySortOrderAsc();
    
    /**
     * 카테고리별 아바타 조회하도록 하는 (고양이 말고도 다른 귀여운 애들을 대비해서 추후 확장용)
     */
    List<Avatar> findByCategoryOrderBySortOrderAsc(String category);
}
