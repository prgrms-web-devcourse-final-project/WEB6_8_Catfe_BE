package com.back.domain.study.record.repository;

import com.back.domain.study.record.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {
    // 시작 시간을 기준으로만 조회
    List<StudyRecord> findByUserIdAndStartTimeBetween(
            Long userId, LocalDateTime start, LocalDateTime end);

    // 시작 시간과 종료 시간을 둘 다 고려하여 조회
    @Query("SELECT sr FROM StudyRecord sr " +
            "WHERE sr.user.id = :userId " +
            "AND (" +
            "  (sr.startTime >= :startOfDay AND sr.startTime < :endOfDay) OR " +
            "  (sr.endTime > :startOfDay AND sr.endTime <= :endOfDay) OR " +
            "  (sr.startTime < :startOfDay AND sr.endTime > :endOfDay)" +
            ") " +
            "ORDER BY sr.startTime DESC")
    List<StudyRecord> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    // 특정 StudyPlan에 대해 특정 날짜에 StudyRecord가 있는지 확인 (일일 목표 달성 알림 체크용)
    @Query("SELECT CASE WHEN COUNT(sr) > 0 THEN true ELSE false END " +
            "FROM StudyRecord sr " +
            "WHERE sr.studyPlan.id = :planId " +
            "AND sr.startTime >= :startOfDay " +
            "AND sr.startTime < :endOfDay")
    boolean existsByStudyPlanIdAndDate(
            @Param("planId") Long planId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
