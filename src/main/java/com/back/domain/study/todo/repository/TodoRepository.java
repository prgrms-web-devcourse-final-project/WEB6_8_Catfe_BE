package com.back.domain.study.todo.repository;

import com.back.domain.study.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUserIdAndDate(Long userId, LocalDate date);
    List<Todo> findByUserId(Long userId);
}
