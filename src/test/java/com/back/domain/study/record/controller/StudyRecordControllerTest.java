package com.back.domain.study.record.controller;

import com.back.domain.study.plan.entity.Color;
import com.back.domain.study.plan.entity.Frequency;
import com.back.domain.study.plan.entity.RepeatRule;
import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.plan.repository.StudyPlanRepository;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("StudyRecordController 테스트")
class StudyRecordControllerTest {
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StudyPlanRepository studyPlanRepository;
    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private StudyPlan singlePlan;
    private StudyPlan dailyPlan;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        testUser = userRepository.save(testUser);

        setupJwtMock(testUser);

        singlePlan = createSinglePlan();
        dailyPlan = createDailyPlan();
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

    private StudyPlan createSinglePlan() {
        StudyPlan plan = new StudyPlan();
        plan.setUser(testUser);
        plan.setSubject("단발성 계획");
        plan.setStartDate(LocalDateTime.of(2025, 10, 1, 10, 0));
        plan.setEndDate(LocalDateTime.of(2025, 10, 1, 12, 0));
        plan.setColor(Color.RED);
        return studyPlanRepository.save(plan);
    }
    private StudyPlan createDailyPlan() {
        StudyPlan plan = new StudyPlan();
        plan.setUser(testUser);
        plan.setSubject("매일 반복 계획");
        plan.setStartDate(LocalDateTime.of(2025, 10, 1, 12, 0));
        plan.setEndDate(LocalDateTime.of(2025, 10, 1, 13, 0));
        plan.setColor(Color.BLUE);

        RepeatRule repeatRule = new RepeatRule();
        repeatRule.setFrequency(Frequency.DAILY);
        repeatRule.setRepeatInterval(1);
        repeatRule.setUntilDate(LocalDateTime.of(2025, 12, 31, 0, 0).toLocalDate());
        repeatRule.setStudyPlan(plan);
        plan.setRepeatRule(repeatRule);

        return studyPlanRepository.save(plan);

    }

    @Test
    @DisplayName("학습 기록 생성 - 일시정지 없음")
    void t1() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/plans/records")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "planId": %d,
                        "startTime": "2025-10-03T10:00:00",
                        "endTime": "2025-10-03T12:00:00",
                        "duration": 7200,
                        "pauseInfos": []
                    }
                    """.formatted(singlePlan.getId())))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(StudyRecordController.class))
                .andExpect(handler().methodName("createStudyRecord"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 기록이 생성되었습니다."))
                .andExpect(jsonPath("$.data.planId").value(singlePlan.getId()))
                .andExpect(jsonPath("$.data.startTime").value("2025-10-03T10:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("2025-10-03T12:00:00"))
                .andExpect(jsonPath("$.data.duration").value(7200))
                .andExpect(jsonPath("$.data.pauseInfos", hasSize(0)));
    }
    @Test
    @DisplayName("학습 기록 생성 - 일시정지 있음")
    void t2() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/plans/records")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "planId": %d,
                                    "startTime": "2025-10-03T14:00:00",
                                    "endTime": "2025-10-03T17:00:00",
                                    "duration": "7500",
                                    "pauseInfos": [
                                        {
                                            "pausedAt": "2025-10-03T15:00:00",
                                            "restartAt": "2025-10-03T15:30:00"
                                        },{
                                            "pausedAt": "2025-10-03T15:50:00",
                                            "restartAt": "2025-10-03T16:10:00"
                                        },{
                                            "pausedAt": "2025-10-03T16:50:00",
                                            "restartAt": "2025-10-03T16:55:00"
                                        }
                                    ]
                                }
                                """.formatted(dailyPlan.getId())))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startTime").value("2025-10-03T14:00:00"))
                .andExpect(jsonPath("$.data.pauseInfos", hasSize(3)))
                .andExpect(jsonPath("$.data.duration").value("7500"));
    }

    @Test
    @DisplayName("학습 기록 생성 - 일시정지 + 마지막 재시작 없음")
    void t2_1() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/plans/records")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "planId": %d,
                                    "startTime": "2025-10-03T14:00:00",
                                    "endTime": "2025-10-03T17:00:00",
                                    "duration": "7200",
                                    "pauseInfos": [
                                        {
                                            "pausedAt": "2025-10-03T15:00:00",
                                            "restartAt": "2025-10-03T15:30:00"
                                        },{
                                            "pausedAt": "2025-10-03T15:50:00",
                                            "restartAt": "2025-10-03T16:10:00"
                                        },{
                                            "pausedAt": "2025-10-03T16:50:00"
                                        }
                                    ]
                                }
                                """.formatted(dailyPlan.getId())))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startTime").value("2025-10-03T14:00:00"))
                .andExpect(jsonPath("$.data.pauseInfos", hasSize(3)))
                .andExpect(jsonPath("$.data.duration").value("7200"));
    }

    @Test
    @DisplayName("학습 기록 조회 - 일시정지 없음")
    void t3() throws Exception {
        mvc.perform(post("/api/plans/records")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "planId": %d,
                                    "startTime": "2025-10-03T10:00:00",
                                    "endTime": "2025-10-03T12:00:00",
                                    "duration": 7200,
                                    "pauseInfos": []
                                }
                                """.formatted(singlePlan.getId())))
                .andExpect(status().isOk());

        // 조회
        ResultActions resultActions = mvc.perform(get("/api/plans/records?date=2025-10-03")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일별 학습 기록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].planId").value(singlePlan.getId()))
                .andExpect(jsonPath("$.data[0].duration").value(7200));
    }

    @Test
    @DisplayName("학습 기록 조회 - 전날 밤~당일 새벽 기록의 경우")
    void t4() throws Exception {
        mvc.perform(post("/api/plans/records")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "planId": %d,
                        "startTime": "2025-10-02T23:00:00",
                        "endTime": "2025-10-03T02:00:00",
                        "duration": 10800,
                        "pauseInfos": []
                    }
                    """.formatted(singlePlan.getId())))
                .andExpect(status().isOk());

        // 10월 2일로 조회 (04:00 기준이므로 이 기록이 포함되어야 함)
        ResultActions resultActions = mvc.perform(get("/api/plans/records?date=2025-10-02")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].startTime").value("2025-10-02T23:00:00"))
                .andExpect(jsonPath("$.data[0].endTime").value("2025-10-03T02:00:00"));
        }

    @Test
    @DisplayName("학습 기록 조회 - 전날 밤~당일 오전 4시 이후 끝난 기록의 경우")
    void t5() throws Exception {
        mvc.perform(post("/api/plans/records")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "planId": %d,
                                    "startTime": "2025-10-02T23:00:00",
                                    "endTime": "2025-10-03T05:00:00",
                                    "duration": 25200,
                                    "pauseInfos": []
                                }
                                """.formatted(singlePlan.getId())))
                .andExpect(status().isOk());
        // 10월 2일 조회
        ResultActions resultActions = mvc.perform(get("/api/plans/records?date=2025-10-02")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].startTime").value("2025-10-02T23:00:00"))
                .andExpect(jsonPath("$.data[0].endTime").value("2025-10-03T05:00:00"))
                .andExpect(jsonPath("$.data[0].duration").value(21600));

        // 10월 3일 조회
        resultActions = mvc.perform(get("/api/plans/records?date=2025-10-03")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].startTime").value("2025-10-02T23:00:00"))
                .andExpect(jsonPath("$.data[0].endTime").value("2025-10-03T02:00:00"))
                .andExpect(jsonPath("$.data[0].duration").value(21600));
    }

}