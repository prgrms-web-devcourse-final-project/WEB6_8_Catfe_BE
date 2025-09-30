package com.back.domain.study.todo.service;

import com.back.domain.study.todo.dto.TodoRequestDto;
import com.back.domain.study.todo.dto.TodoResponseDto;
import com.back.domain.study.todo.entity.Todo;
import com.back.domain.study.todo.repository.TodoRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    // ==================== 생성 ===================
    public TodoResponseDto createTodo(Long userId, TodoRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Todo todo = new Todo(
                user,
                request.description(),
                request.date()
        );

        Todo savedTodo = todoRepository.save(todo);
        return TodoResponseDto.from(savedTodo);
    }

    // ==================== 조회 ===================
    // 유저의 특정 날짜의 모든 할 일 조회
    public List<TodoResponseDto> getTodosByDate(Long userId, LocalDate date) {
        List<Todo> todos = todoRepository.findByUserIdAndDate(userId, date);
        return todos.stream()
                .map(TodoResponseDto::from)
                .collect(Collectors.toList());
    }
    // 유저의 전체 할 일 조회
    public List<TodoResponseDto> getAllTodos(Long userId) {
        List<Todo> todos = todoRepository.findByUserId(userId);
        return todos.stream()
                .map(TodoResponseDto::from)
                .collect(Collectors.toList());
    }
    // ==================== 수정 ===================

    // ==================== 삭제 ===================
}
