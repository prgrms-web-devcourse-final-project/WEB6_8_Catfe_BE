package com.back.domain.study.todo.controller;

import com.back.domain.study.todo.entity.Todo;
import com.back.domain.study.todo.repository.TodoRepository;
import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.security.user.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TodoController 테스트")
class TodoControllerTest {

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User otherUser;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 9, 30);

        testUser = User.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        testUser = userRepository.save(testUser);

        otherUser = User.builder()
                .email("other@example.com")
                .username("otheruser")
                .password("password123")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        otherUser = userRepository.save(otherUser);

        setupJwtMock(testUser);
    }

    private void setupJwtMock(User user) {
        given(jwtTokenProvider.validateAccessToken(anyString())).willReturn(true);

        CustomUserDetails userDetails = CustomUserDetails.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();

        given(jwtTokenProvider.getAuthentication(anyString()))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                ));
    }

    private Todo createTodo(String description, LocalDate date) {
        Todo todo = new Todo(testUser, description, date);
        return todoRepository.save(todo);
    }

    // ==================== 생성 테스트 ====================

    @Test
    @DisplayName("할 일 생성 성공")
    void t1() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/todos")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "새로운 할일",
                                    "date": "2025-09-30"
                                }
                                """))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(TodoController.class))
                .andExpect(handler().methodName("createTodo"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("할 일이 생성되었습니다."))
                .andExpect(jsonPath("$.data.description").value("새로운 할일"))
                .andExpect(jsonPath("$.data.date").value("2025-09-30"))
                .andExpect(jsonPath("$.data.isComplete").value(false));
    }

    @Test
    @DisplayName("할 일 생성 실패 - description 필드 누락")
    void t2() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/todos")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "date": "2025-09-30"
                                }
                                """))
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    @DisplayName("할 일 생성 실패 - date 필드 누락")
    void t3() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/todos")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "할일"
                                }
                                """))
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== 조회 테스트 ====================

    @Test
    @DisplayName("특정 날짜 할 일 조회 성공")
    void t4() throws Exception {
        createTodo("할일 1", testDate);
        createTodo("할일 2", testDate);
        todoRepository.flush();

        ResultActions resultActions = mvc.perform(get("/api/todos")
                        .header("Authorization", "Bearer faketoken")
                        .param("date", "2025-09-30"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(TodoController.class))
                .andExpect(handler().methodName("getTodos"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("할 일 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].description").value("할일 1"))
                .andExpect(jsonPath("$.data[1].description").value("할일 2"));
    }

    @Test
    @DisplayName("특정 날짜 할 일 조회 - 빈 리스트")
    void t5() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/todos")
                        .header("Authorization", "Bearer faketoken")
                        .param("date", "2025-09-01"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("전체 할 일 조회 성공")
    void t6() throws Exception {
        createTodo("할일 1", testDate);
        createTodo("할일 2", testDate.plusDays(1));
        todoRepository.flush();

        // 임시: 전체 데이터 수 확인
        long totalCount = todoRepository.count();
        System.out.println("Total todos in DB: " + totalCount);

        // testUser의 Todo만 확인
        List<Todo> userTodos = todoRepository.findByUserId(testUser.getId());
        System.out.println("User todos: " + userTodos.size());

        ResultActions resultActions = mvc.perform(get("/api/todos/all")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        // 일단 상태만 확인
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("할 일 조회 실패 - 잘못된 날짜 형식")
    void t7() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/todos")
                        .header("Authorization", "Bearer faketoken")
                        .param("date", "2025-13-40"))
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== 수정 테스트 ====================

    @Test
    @DisplayName("할 일 수정 성공")
    void t8() throws Exception {
        Todo todo = createTodo("원래 할일", testDate);
        Long todoId = todo.getId();

        ResultActions resultActions = mvc.perform(put("/api/todos/{todoId}", todoId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "수정된 할일",
                                    "date": "2025-09-30"
                                }
                                """))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(TodoController.class))
                .andExpect(handler().methodName("updateTodo"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("할 일이 수정되었습니다."))
                .andExpect(jsonPath("$.data.id").value(todoId))
                .andExpect(jsonPath("$.data.description").value("수정된 할일"));
    }

    @Test
    @DisplayName("할 일 수정 실패 - 존재하지 않는 할일")
    void t9() throws Exception {
        ResultActions resultActions = mvc.perform(put("/api/todos/999")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "수정 시도",
                                    "date": "2025-09-30"
                                }
                                """))
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_001"));
    }

    @Test
    @DisplayName("할 일 수정 실패 - 권한 없음")
    void t10() throws Exception {
        Todo todo = createTodo("할일", testDate);
        Long todoId = todo.getId();

        setupJwtMock(otherUser);

        ResultActions resultActions = mvc.perform(put("/api/todos/{todoId}", todoId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "수정 시도",
                                    "date": "2025-09-30"
                                }
                                """))
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_002"));
    }

    // ==================== 완료 상태 토글 테스트 ====================

    @Test
    @DisplayName("할 일 완료 상태 토글 성공 - 미완료 -> 완료")
    void t11() throws Exception {
        Todo todo = createTodo("할일", testDate);
        Long todoId = todo.getId();

        ResultActions resultActions = mvc.perform(put("/api/todos/{todoId}/toggle", todoId)
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(TodoController.class))
                .andExpect(handler().methodName("toggleTodoComplete"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("할 일 상태가 변경되었습니다."))
                .andExpect(jsonPath("$.data.isComplete").value(true));
    }

    @Test
    @DisplayName("할 일 완료 상태 토글 - 완료 -> 미완료")
    void t12() throws Exception {
        Todo todo = createTodo("할일", testDate);
        todo.toggleComplete();
        todoRepository.save(todo);
        Long todoId = todo.getId();

        ResultActions resultActions = mvc.perform(put("/api/todos/{todoId}/toggle", todoId)
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isComplete").value(false));
    }

    @Test
    @DisplayName("할 일 완료 상태 토글 실패 - 존재하지 않는 할일")
    void t13() throws Exception {
        ResultActions resultActions = mvc.perform(put("/api/todos/999/toggle")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_001"));
    }

    // ==================== 삭제 테스트 ====================

    @Test
    @DisplayName("할 일 삭제 성공")
    void t14() throws Exception {
        Todo todo = createTodo("할일", testDate);
        Long todoId = todo.getId();

        ResultActions resultActions = mvc.perform(delete("/api/todos/{todoId}", todoId)
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(TodoController.class))
                .andExpect(handler().methodName("deleteTodo"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("할 일이 삭제되었습니다."));

        boolean exists = todoRepository.existsById(todoId);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("할 일 삭제 실패 - 존재하지 않는 할일")
    void t15() throws Exception {
        ResultActions resultActions = mvc.perform(delete("/api/todos/999")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_001"));
    }

    @Test
    @DisplayName("할 일 삭제 실패 - 권한 없음")
    void t16() throws Exception {
        Todo todo = createTodo("할일", testDate);
        Long todoId = todo.getId();

        setupJwtMock(otherUser);

        ResultActions resultActions = mvc.perform(delete("/api/todos/{todoId}", todoId)
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TODO_002"));
    }
}