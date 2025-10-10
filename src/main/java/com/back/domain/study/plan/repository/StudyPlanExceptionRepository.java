package com.back.domain.study.plan.repository;

import com.back.domain.study.plan.entity.ApplyScope;
import com.back.domain.study.plan.entity.StudyPlanException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyPlanExceptionRepository extends JpaRepository<StudyPlanException, Long> {
    // FROM_THIS_DATE 범위의 예외들 중 특정 날짜 이전의 예외 조회 (메서드 이름으로 쿼리 생성)
    List<StudyPlanException> findByStudyPlanIdAndApplyScopeAndExceptionDateLessThanEqual(
            Long studyPlanId,
            ApplyScope applyScope,
            LocalDate exceptionDate
    );

    // 특정 계획의 특정 날짜 예외 조회
    @Query("SELECT spe FROM StudyPlanException spe WHERE spe.studyPlan.id = :planId " +
            "AND DATE(spe.exceptionDate) = DATE(:targetDate)")
    Optional<StudyPlanException> findByPlanIdAndDate(@Param("planId") Long planId,
                                                     @Param("targetDate") LocalDate targetDate);

    // 특정 계획의 특정 날짜 이후 예외 모두 삭제
    void deleteByStudyPlanIdAndExceptionDateGreaterThanEqual(Long studyPlanId, LocalDate date);
}
