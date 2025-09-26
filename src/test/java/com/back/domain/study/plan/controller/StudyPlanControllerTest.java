package com.back.domain.study.plan.controller;

import com.back.domain.study.plan.dto.StudyPlanRequest;
import com.back.domain.study.plan.entity.Color;
import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.plan.repository.StudyPlanRepository;
import com.back.domain.study.plan.service.StudyPlanService;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.security.CustomUserDetails;
import com.back.global.security.JwtTokenProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudyPlanControllerTest {

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test1@example.com")
                .username("user1")
                .password("123456789")
                .build();
        testUser = userRepository.save(testUser);

        // JWT Mock을 setUp에서 한 번만 설정
        setupJwtMock(testUser);
    }

    // JWT Mock 설정을 위한 헬퍼 메소드
    private void setupJwtMock(User user) {
        given(jwtTokenProvider.validateToken(anyString())).willReturn(true);

        CustomUserDetails userDetails = CustomUserDetails.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role("USER")
                .build();

        given(jwtTokenProvider.getAuthentication(anyString()))
                .willReturn(new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                ));
    }

    @Test
    @DisplayName("단발성 계획 생성")
    void t1() throws Exception {

        ResultActions resultActions = mvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "단발성 계획",
                        "startDate": "2025-09-26T10:46:12",
                        "endDate": "2025-09-26T11:46:12",
                        "color": "RED"
                    }
                    """))
                .andDo(print());



        // 추가 검증
        resultActions
                .andExpect(status().isOk()) // 200 OK인지 확인
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(handler().methodName("createStudyPlan"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.subject").value("단발성 계획"))
                .andExpect(jsonPath("$.data.color").value("RED"))
                .andExpect(jsonPath("$.data.startDate").value("2025-09-26T10:46:12"))
                .andExpect(jsonPath("$.data.endDate").value("2025-09-26T11:46:12"))
                .andExpect(jsonPath("$.data.repeatRule").doesNotExist());

    }

    @Test
    @DisplayName("반복성 계획 생성")
    void t2() throws Exception {

        ResultActions resultActions = mvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                    {
                        "subject": "반복 계획",
                        "startDate": "2025-09-26T10:46:12",
                        "endDate": "2025-09-26T11:46:12",
                        "color": "BLUE",
                        "repeatRule": {
                            "frequency": "WEEKLY",
                            "repeatInterval": 1,
                            "byDay": "FRI",
                            "untilDate": "2025-12-31"
                        }
                    }
                    """))
                    .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK인지 확인
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(handler().methodName("createStudyPlan"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.subject").value("반복 계획"))
                .andExpect(jsonPath("$.data.color").value("BLUE"))
                .andExpect(jsonPath("$.data.startDate").value("2025-09-26T10:46:12"))
                .andExpect(jsonPath("$.data.endDate").value("2025-09-26T11:46:12"))
                .andExpect(jsonPath("$.data.repeatRule.frequency").value("WEEKLY"))
                .andExpect(jsonPath("$.data.repeatRule.repeatInterval").value(1))
                .andExpect(jsonPath("$.data.repeatRule.byDay").value("FRI"))
                .andExpect(jsonPath("$.data.repeatRule.untilDate").value("2025-12-31"));

    }
}