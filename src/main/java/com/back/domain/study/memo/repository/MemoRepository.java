package com.back.domain.study.memo.repository;

import com.back.domain.study.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MemoRepository extends JpaRepository<Memo, Long> {
    // 사용자의 날짜별 메모 조회
    Optional<Memo> findByUserIdAndDate(Long userId, LocalDate date);

}
