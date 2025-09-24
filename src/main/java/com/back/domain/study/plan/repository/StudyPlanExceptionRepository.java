package com.back.domain.study.plan.repository;

import com.back.domain.study.plan.entity.StudyPlanException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudyPlanExceptionRepository extends JpaRepository<StudyPlanException, Long> {
    // FROM_THIS_DATE 범위의 예외들 중 특정 날짜 이전의 예외 조회
    @Query("SELECT spe FROM StudyPlanException spe WHERE spe.studyPlan.id = :planId " +
            "AND spe.applyScope = :applyScope " +
            "AND spe.exceptionDate <= :targetDate " +
            "ORDER BY spe.exceptionDate DESC")
    List<StudyPlanException> findByStudyPlanIdAndApplyScopeAndExceptionDateBefore(
            @Param("planId") Long planId,
            @Param("applyScope") StudyPlanException.ApplyScope applyScope,
            @Param("targetDate") LocalDateTime targetDate);
    //특정 계획의 특정 기간 예외 조회
    @Query("SELECT spe FROM StudyPlanException spe WHERE spe.studyPlan.id = :planId " +
            "AND spe.exceptionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY spe.exceptionDate")
    List<StudyPlanException> findByStudyPlanIdAndExceptionDateBetween(@Param("planId") Long planId,
                                                                      @Param("startDate") LocalDateTime startDate,
                                                                      @Param("endDate") LocalDateTime endDate);
}
