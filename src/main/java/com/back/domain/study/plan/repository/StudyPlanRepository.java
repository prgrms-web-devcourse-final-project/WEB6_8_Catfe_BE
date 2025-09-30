package com.back.domain.study.plan.repository;

import com.back.domain.study.plan.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByUserId(Long userId);
    /* 시간 겹침 조건:
     새 계획 시작 시간보다 기존 계획 종료 시간이 늦고 (p.endDate > :newStart),
     새 계획 종료 시간보다 기존 계획 시작 시간이 빨라야 한다 (p.startDate < :newEnd).
     (종료 시간 == 새 시작 시간)은 허용
     */
    @Query("""
    SELECT p 
    FROM StudyPlan p 
    WHERE p.user.id = :userId 
      AND (:planIdToExclude IS NULL OR p.id != :planIdToExclude)
      AND p.endDate > :newStart 
      AND p.startDate < :newEnd
""")
    List<StudyPlan> findByUserIdAndNotIdAndOverlapsTime(
            Long userId,
            Long planIdToExclude,
            LocalDateTime newStart,
            LocalDateTime newEnd
    );
}
