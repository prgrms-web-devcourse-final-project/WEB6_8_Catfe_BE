package com.back.domain.study.plan.controller;

import com.back.domain.study.plan.dto.StudyPlanRequest;
import com.back.domain.study.plan.entity.Color;
import com.back.domain.study.plan.entity.Frequency;
import com.back.domain.study.plan.entity.RepeatRule;
import com.back.domain.study.plan.entity.StudyPlan;
import com.back.domain.study.plan.repository.StudyPlanRepository;
import com.back.domain.study.plan.service.StudyPlanService;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.security.jwt.JwtTokenProvider;
import com.back.global.security.user.CustomUserDetails;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private StudyPlan savedStudyPlan;

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

        // 저장된 StudyPlan 설정
        savedStudyPlan = new StudyPlan();
        savedStudyPlan.setUser(testUser);
        savedStudyPlan.setSubject("Java 공부");
        savedStudyPlan.setStartDate(LocalDateTime.of(2025, 1, 15, 9, 0, 0));
        savedStudyPlan.setEndDate(LocalDateTime.of(2025, 1, 15, 11, 0, 0));
        savedStudyPlan.setColor(Color.BLUE);
    }

    // JWT Mock 설정을 위한 헬퍼 메소드
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
    @DisplayName("단발성 계획 생성")
    void t1() throws Exception {

        ResultActions resultActions = mvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "단발성 계획",
                        "startDate": "2025-09-26T10:46:00",
                        "endDate": "2025-09-26T11:46:00",
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
                .andExpect(jsonPath("$.data.startDate").value("2025-09-26T10:46:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-09-26T11:46:00"))
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
                        "startDate": "2025-09-26T10:46:00",
                        "endDate": "2025-09-26T11:46:00",
                        "color": "BLUE",
                        "repeatRule": {
                            "frequency": "DAILY",
                            "repeatInterval": 1,
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
                .andExpect(jsonPath("$.data.startDate").value("2025-09-26T10:46:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-09-26T11:46:00"))
                .andExpect(jsonPath("$.data.repeatRule.frequency").value("DAILY"))
                .andExpect(jsonPath("$.data.repeatRule.intervalValue").value(1))
                .andExpect(jsonPath("$.data.repeatRule.byDay", hasSize(0)))
                .andExpect(jsonPath("$.data.repeatRule.untilDate").value("2025-12-31"));

    }

    @Test
    @DisplayName("계획 생성 - 잘못된 요청 (종료 시간이 시작 시간보다 빠름)")
    void t3() throws Exception {

        ResultActions resultActions = mvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "반복 계획",
                        "startDate": "2025-09-26T11:46:00",
                        "endDate": "2025-09-26T10:46:00",
                        "color": "BLUE",
                        "repeatRule": {
                            "frequency": "WEEKLY",
                            "repeatInterval": 1,
                            "byDay": ["FRI"],
                            "untilDate": "2025-12-31"
                        }
                    }
                    """))
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(handler().methodName("createStudyPlan"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PLAN_006"))
                .andExpect(jsonPath("$.message").value("시작 시간은 종료 시간보다 빨라야 합니다."));
    }

    @Test
    @DisplayName("계획 생성 - 잘못된 요청 (반복 규칙의 종료 날짜가 계획의 종료 날짜보다 빠름)")
    void t4() throws Exception {

        ResultActions resultActions = mvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""

                                {
                        "subject": "반복 계획",
                        "startDate": "2025-09-26T10:46:00",
                        "endDate": "2025-09-26T11:46:00",
                        "color": "BLUE",
                        "repeatRule": {
                            "frequency": "WEEKLY",
                            "repeatInterval": 1,
                            "byDay": ["FRI"],
                            "untilDate": "2025-09-25"
                        }
                    }
                    """))
                .andDo(print());
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(handler().methodName("createStudyPlan"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPEAT_001"));
        }

    @Test
    @DisplayName("계획 조회 - 특정 날짜")
     void t5() throws Exception {
        // 테스트용 계획 저장
            StudyPlan planToSave = new StudyPlan();
            planToSave.setUser(testUser);
            planToSave.setSubject("Java 공부");
            planToSave.setStartDate(LocalDateTime.of(2025, 9, 29, 9, 0, 0));
            planToSave.setEndDate(LocalDateTime.of(2025, 9, 29, 11, 0, 0));
            planToSave.setColor(Color.BLUE);

            studyPlanRepository.save(planToSave);
            studyPlanRepository.flush();

            ResultActions resultActions = mvc.perform(get("/api/plans/date/2025-09-29")
                            .header("Authorization", "Bearer faketoken")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            resultActions
                    .andExpect(status().isOk()) // 200 OK인지 확인
                    .andExpect(handler().handlerType(StudyPlanController.class))
                    .andExpect(handler().methodName("getStudyPlansForDate"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("해당 날짜의 계획을 조회했습니다."))
                    .andExpect(jsonPath("$.data.date").value("2025-09-29"))
                    .andExpect(jsonPath("$.data.totalCount").value(1))
                    .andExpect(jsonPath("$.data.plans", hasSize(1)))
                    .andExpect(jsonPath("$.data.plans[0].subject").value("Java 공부"))
                    .andExpect(jsonPath("$.data.plans[0].color").value("BLUE"))
                    .andExpect(jsonPath("$.data.plans[0].startDate").value("2025-09-29T09:00:00"))
                    .andExpect(jsonPath("$.data.plans[0].endDate").value("2025-09-29T11:00:00"))
                    .andExpect(jsonPath("$.data.plans[0].repeatRule").doesNotExist());
        }
    @Test
    @DisplayName("계획 조회 - 특정 날짜 (계획 없음) ")
    void t6() throws Exception {

        ResultActions resultActions = mvc.perform(get("/api/plans/date/2025-09-01")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
        resultActions
                .andExpect(status().isOk()) // 200 OK인지 확인
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(handler().methodName("getStudyPlansForDate"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("해당 날짜의 계획을 조회했습니다."))
                .andExpect(jsonPath("$.data.date").value("2025-09-01"))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.plans", hasSize(0)));
    }
    @Test
    @DisplayName("계획 조회 - 기간별 조회")
    void t7() throws Exception {
        // 테스트용 계획 저장
        StudyPlan planToSave1 = new StudyPlan();
        planToSave1.setUser(testUser);
        planToSave1.setSubject("Java 공부");
        planToSave1.setStartDate(LocalDateTime.of(2025, 9, 15, 9, 0, 0));
        planToSave1.setEndDate(LocalDateTime.of(2025, 9, 15, 11, 0, 0));
        planToSave1.setColor(Color.BLUE);

        StudyPlan planToSave2 = new StudyPlan();
        planToSave2.setUser(testUser);
        planToSave2.setSubject("Spring 공부");
        planToSave2.setStartDate(LocalDateTime.of(2025, 9, 20, 9, 0, 0));
        planToSave2.setEndDate(LocalDateTime.of(2025, 9, 20, 11, 0, 0));
        planToSave2.setColor(Color.RED);
        RepeatRule repeatRule = new RepeatRule();
        repeatRule.setFrequency(Frequency.DAILY);
        repeatRule.setRepeatInterval(1);
        planToSave2.setRepeatRule(repeatRule);
        repeatRule.setStudyPlan(planToSave2);

        studyPlanRepository.saveAll(List.of(planToSave1, planToSave2));
        studyPlanRepository.flush();

        ResultActions resultActions = mvc.perform(get("/api/plans?start=2025-09-10&end=2025-09-25")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK인지 확인
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(handler().methodName("getStudyPlansForPeriod"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("기간별 계획을 조회했습니다."))
                .andExpect(jsonPath("$.data", hasSize(7)));
    }

    @Test
    @DisplayName("계획 조회 - 날짜 형식 에러")
    void t8() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/plans/date/2025-9-29")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PLAN_005")) // 날짜 형식 오류에 해당하는 커스텀 코드
                .andExpect(jsonPath("$.message").value("날짜 형식이 올바르지 않습니다. (YYYY-MM-DD 형식을 사용해주세요)"));
    }

    @Test
    @DisplayName("계획 수정 - 단발성")
    void t9() throws Exception {
        StudyPlan originalPlan = createSinglePlan();
        Long planId = originalPlan.getId();

        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "subject": "수정된 최종 계획 (PUT)",
                            "startDate": "2025-10-10T14:00:00",
                            "endDate": "2025-10-10T16:00:00",
                            "color": "RED"
                        }
                        """))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 수정되었습니다.")) // 예상 응답 메시지
                .andExpect(jsonPath("$.data.id").value(planId))
                .andExpect(jsonPath("$.data.subject").value("수정된 최종 계획 (PUT)"))
                .andExpect(jsonPath("$.data.color").value("RED"))
                .andExpect(jsonPath("$.data.startDate").value("2025-10-10T14:00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-10-10T16:00:00"))
                .andExpect(jsonPath("$.data.repeatRule").doesNotExist());

    }

    @Test
    @DisplayName("계획 수정 - 단발성 (반복 규칙 추가 시도 + WEEKLY 인 경우 byDay 자동 적용)")
    void t9_1() throws Exception {
        StudyPlan originalPlan = createSinglePlan();
        Long planId = originalPlan.getId();

        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "subject": "수정된 최종 계획 (PUT)",
                            "startDate": "2025-10-01T10:00:00",
                            "endDate": "2025-10-01T12:00:00",
                            "color": "RED",
                            "repeatRule": {
                                "frequency": "WEEKLY",
                                "repeatInterval": 1,
                                "untilDate": "2025-12-31"
                            }
                        }
                        """))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 400 Bad Request
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subject").value("수정된 최종 계획 (PUT)"))
                .andExpect(jsonPath("$.data.repeatRule.byDay", hasSize(1)))
                .andExpect(jsonPath("$.data.repeatRule.byDay[0]").value("WED"));
    }

    @Test
    @DisplayName("계획 수정 - 반복성 가상 계획 단일 수정")
    void t10() throws Exception {
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();
        // 반복 주기 제외 수정
        // 반복 주기의 변경사항이 없다면 RepeatRule은 안보내도 된다.
        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "subject": "수정된 반복 계획 (PUT)",
                            "startDate": "2025-10-10T14:00:00",
                            "endDate": "2025-10-10T16:00:00",
                            "color": "BLUE"
                        }
                        """))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 수정되었습니다.")) // 예상 응답 메시지
                .andExpect(jsonPath("$.data.id").value(planId))
                .andExpect(jsonPath("$.data.subject").value("수정된 반복 계획 (PUT)"))
                .andExpect(jsonPath("$.data.color").value("BLUE"))
                .andExpect(jsonPath("$.data.startDate").value("2025-10-10T14:00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-10-10T16:00:00"));
        //원본은 변경사항 없이 조회가 잘 되는지도 검증
        mvc.perform(get("/api/plans/date/2025-10-01")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data.plans[0].subject").value("매일 반복 계획"));
    }

    @Test
    @DisplayName("계획 수정 - 반복성 일괄 수정, 반복 규칙 그대로")
    void t11() throws Exception {
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();

        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=FROM_THIS_DATE", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "subject": "수정된 반복 계획 (PUT)",
                            "startDate": "2025-10-10T14:00:00",
                            "endDate": "2025-10-10T16:00:00",
                            "color": "BLUE"
                        }
                        """))
                .andDo(print());

        // 10일 날짜 전후를 검색하여 수정이 잘 되었는지 검증
        mvc.perform(get("/api/plans/date/2025-10-12")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data.plans[0].subject").value("수정된 반복 계획 (PUT)"));

        mvc.perform(get("/api/plans/date/2025-10-09")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data.plans[0].subject").value("매일 반복 계획"));
    }


    @Test
    @DisplayName("계획 삭제 - 단발성 단독 삭제")
    void t12() throws Exception {
        StudyPlan originalPlan = createSinglePlan();
        Long planId = originalPlan.getId();

        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.delete("/api/plans/{planId}?selectedDate=2025-10-01&applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 삭제되었습니다."))
                .andExpect(jsonPath("$.data.id").value(planId))
                .andExpect(jsonPath("$.data.subject").value(originalPlan.getSubject()))
                .andExpect(jsonPath("$.data.color").value(Color.RED.name()))
                .andExpect(jsonPath("$.data.deletedDate").value("2025-10-01"))
                .andExpect(jsonPath("$.data.applyScope").value("THIS_ONLY"));

        // DB에서 실제로 삭제되었는지 확인
        boolean exists = studyPlanRepository.existsById(planId);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("계획 삭제 - 반복성 단일 삭제")
    void t13() throws Exception {
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();

        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.delete("/api/plans/{planId}?selectedDate=2025-10-01&applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 삭제되었습니다."))
                .andExpect(jsonPath("$.data.id").value(planId))
                .andExpect(jsonPath("$.data.subject").value(originalPlan.getSubject()))
                .andExpect(jsonPath("$.data.deletedDate").value("2025-10-01"))
                .andExpect(jsonPath("$.data.applyScope").value("THIS_ONLY"))
                .andExpect(jsonPath("$.data.startDate").value("2025-10-01T12:00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-10-01T13:00:00"));

        // 10월 1일에 해당하는 계획은 없어야함
        mvc.perform(get("/api/plans/date/2025-10-01")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data.totalCount").value(0));

        // 10월 10일에 해당하는 계획은 있어야함
        mvc.perform(get("/api/plans/date/2025-10-10")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @DisplayName("계획 삭제 - 반복성 원본 일괄 삭제")
    void t14() throws Exception {
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();

        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.delete("/api/plans/{planId}?selectedDate=2025-10-01&applyScope=FROM_THIS_DATE", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 삭제되었습니다."))
                .andExpect(jsonPath("$.data.id").value(planId))
                .andExpect(jsonPath("$.data.deletedDate").value("2025-10-01"))
                .andExpect(jsonPath("$.data.applyScope").value("FROM_THIS_DATE"));

        // 10월 10일에 해당하는 계획도 없어야함
        mvc.perform(get("/api/plans/date/2025-10-10")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("계획 삭제 - 반복성 가상 일괄삭제")
    void t15() throws Exception {
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();

        ResultActions resultActions = mvc.perform(MockMvcRequestBuilders.delete("/api/plans/{planId}?selectedDate=2025-10-10&applyScope=FROM_THIS_DATE", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 삭제되었습니다."))
                .andExpect(jsonPath("$.data.id").value(planId))
                .andExpect(jsonPath("$.data.deletedDate").value("2025-10-10"))
                .andExpect(jsonPath("$.data.applyScope").value("FROM_THIS_DATE"))
                .andExpect(jsonPath("$.data.startDate").value("2025-10-10T12:00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-10-10T13:00:00"));

        // 10월 1일에 해당하는 계획은 있어야함
        mvc.perform(get("/api/plans/date/2025-10-01")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(jsonPath("$.data.totalCount").value(1));

        // 10.10 이후에 해당하는 계획은 없어야함
        mvc.perform(get("/api/plans?start=2025-10-10&end=2025-10-15")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                //검색 결과가 없다면 빈 배열
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

}