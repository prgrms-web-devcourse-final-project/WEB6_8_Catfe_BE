package com.back.domain.study.record.repository;

import com.back.domain.study.record.entity.StudyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {
    List<StudyRecord> findByUserIdAndStartTimeBetween(
            Long userId, LocalDateTime start, LocalDateTime end);
}
