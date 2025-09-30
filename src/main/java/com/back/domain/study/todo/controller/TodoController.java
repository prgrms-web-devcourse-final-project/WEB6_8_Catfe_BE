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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/todos")
@Tag(name = "Todo", description = "할 일 관련 API")
public class TodoController {
    private final TodoService todoService;

    @PostMapping
    @Operation(summary = "할 일 생성", description = "새로운 할 일을 생성합니다.")
    public ResponseEntity<RsData<TodoResponseDto>> createTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TodoRequestDto requestDto
    ) {
        TodoResponseDto response = todoService.createTodo(userDetails.getUserId(), requestDto);
        return ResponseEntity.ok(RsData.success("할 일이 생성되었습니다.", response));
    }

}
