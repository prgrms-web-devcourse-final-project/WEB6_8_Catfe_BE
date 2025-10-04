package com.back.domain.study.todo.controller;

import com.back.domain.study.todo.dto.TodoRequestDto;
import com.back.domain.study.todo.dto.TodoResponseDto;
import com.back.domain.study.todo.service.TodoService;
import com.back.global.common.dto.RsData;
import com.back.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
@Tag(name = "Todo", description = "할 일 관련 API")
public class TodoController {
    private final TodoService todoService;

    // ==================== 생성 ===================
    @PostMapping
    @Operation(summary = "할 일 생성", description = "새로운 할 일을 생성합니다.")
    public ResponseEntity<RsData<TodoResponseDto>> createTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TodoRequestDto requestDto
    ) {
        TodoResponseDto response = todoService.createTodo(userDetails.getUserId(), requestDto);
        return ResponseEntity.ok(RsData.success("할 일이 생성되었습니다.", response));
    }

    // ==================== 조회 ===================
    // 특정 날짜 조회
    @GetMapping
    @Operation(summary = "할 일 목록 조회", description = "조건에 따라 할 일 목록을 조회합니다. " +
            "date만 제공시 해당 날짜, startDate와 endDate 제공시 기간별, 아무것도 없으면 전체 조회")
    public ResponseEntity<RsData<List<TodoResponseDto>>> getTodos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TodoResponseDto> response = todoService.getTodosByDate(userDetails.getUserId(), date);

        return ResponseEntity.ok(RsData.success("할 일 목록을 조회했습니다.", response));
    }

    // 사용자의 모든 할 일 조회
    @GetMapping("/all")
    @Operation(summary = "모든 할 일 조회", description = "사용자의 모든 할 일을 조회합니다.")
    public ResponseEntity<RsData<List<TodoResponseDto>>> getAllTodos(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<TodoResponseDto> response = todoService.getAllTodos(userDetails.getUserId());
        return ResponseEntity.ok(RsData.success("모든 할 일을 조회했습니다.", response));
    }

    // ==================== 수정 ===================
    // 할 일 내용 수정
    @PutMapping("/{todoId}")
    @Operation(summary = "할 일 수정", description = "할 일의 내용과 날짜를 수정합니다.")
    public ResponseEntity<RsData<TodoResponseDto>> updateTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoRequestDto requestDto
    ) {
        TodoResponseDto response = todoService.updateTodo(userDetails.getUserId(), todoId, requestDto);
        return ResponseEntity.ok(RsData.success("할 일이 수정되었습니다.", response));
    }

    // 할 일 완료/미완료 토글
    @PutMapping("/{todoId}/toggle")
    @Operation(summary = "할 일 완료 상태 토글", description = "할 일의 완료 상태를 변경합니다.")
    public ResponseEntity<RsData<TodoResponseDto>> toggleTodoComplete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long todoId
    ) {
        TodoResponseDto response = todoService.toggleTodoComplete(userDetails.getUserId(), todoId);
        return ResponseEntity.ok(RsData.success("할 일 상태가 변경되었습니다.", response));
    }

    // ==================== 삭제 ===================
    // 할 일 삭제
    @DeleteMapping("/{todoId}")
    @Operation(summary = "할 일 삭제", description = "할 일을 삭제합니다.")
    public ResponseEntity<RsData<Void>> deleteTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long todoId
    ) {
        todoService.deleteTodo(userDetails.getUserId(), todoId);
        return ResponseEntity.ok(RsData.success("할 일이 삭제되었습니다."));
    }

}
