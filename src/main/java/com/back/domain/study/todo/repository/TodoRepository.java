package com.back.domain.study.todo.repository;

import com.back.domain.study.todo.entity.Todo;
import com.back.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUserIdAndDate(Long userId, LocalDate date);
    List<Todo> findByUserId(Long userId);
}
