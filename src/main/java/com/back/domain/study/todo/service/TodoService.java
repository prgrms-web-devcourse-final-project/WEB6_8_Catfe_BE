package com.back.domain.study.todo.service;

import com.back.domain.study.todo.dto.TodoRequestDto;
import com.back.domain.study.todo.dto.TodoResponseDto;
import com.back.domain.study.todo.entity.Todo;
import com.back.domain.study.todo.repository.TodoRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
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
    @Transactional
    public TodoResponseDto createTodo(Long userId, TodoRequestDto request) {
        User user = findUserById(userId);

        Todo todo = new Todo(
                user,
                request.description(),
                request.date()
        );

        Todo savedTodo = todoRepository.save(todo);
        return TodoResponseDto.from(savedTodo);
    }

    // ==================== 조회 ===================
    //유저의 특정 날짜의 모든 할 일 조회
    public List<TodoResponseDto> getTodosByDate(Long userId, LocalDate date) {
        List<Todo> todos = todoRepository.findByUserIdAndDate(userId, date);
        return todos.stream()
                .map(TodoResponseDto::from)
                .collect(Collectors.toList());
    }

    //유저의 전체 할 일 조회
    public List<TodoResponseDto> getAllTodos(Long userId) {
        List<Todo> todos = todoRepository.findByUserId(userId);
        return todos.stream()
                .map(TodoResponseDto::from)
                .collect(Collectors.toList());
    }

    // ==================== 수정 ===================
    // 할 일 내용 수정
    @Transactional
    public TodoResponseDto updateTodo(Long userId, Long todoId, TodoRequestDto requestDto) {
        User user = findUserById(userId);
        Todo todo = findTodoByIdAndUser(todoId, user);

        todo.updateDescription(requestDto.description());

        return TodoResponseDto.from(todo);
    }

    // 할 일 완료 상태 토글
    @Transactional
    public TodoResponseDto toggleTodoComplete(Long userId, Long todoId) {
        User user = findUserById(userId);
        Todo todo = findTodoByIdAndUser(todoId, user);
        todo.toggleComplete();
        return TodoResponseDto.from(todo);
    }

    // ==================== 삭제 ===================
    // 할 일 삭제
    @Transactional
    public void deleteTodo(Long userId, Long todoId) {
        User user = findUserById(userId);
        Todo todo = findTodoByIdAndUser(todoId, user);
        todoRepository.delete(todo);
    }

    // ==================== 유틸 ===================
    // 유저 조회
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // 할 일 조회 및 사용자 소유 검증
    private Todo findTodoByIdAndUser(Long todoId, User user) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        if (!todo.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.TODO_FORBIDDEN);
        }

        return todo;
    }
}