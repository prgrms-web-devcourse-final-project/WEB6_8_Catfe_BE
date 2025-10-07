package com.back.domain.study.plan.controller;

import com.back.domain.study.plan.dto.StudyPlanRequest;
import com.back.domain.study.plan.entity.*;
import com.back.domain.study.plan.repository.StudyPlanExceptionRepository;
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

import java.time.LocalDate;
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
    private StudyPlanExceptionRepository studyPlanExceptionRepository;

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
    @DisplayName("단발성 계획 생성 - 하나에만 밀리초가 들어가도 되는가?!")
    void t1() throws Exception {

        ResultActions resultActions = mvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "단발성 계획",
                        "startDate": "2025-10-03T17:00:00",
                        "endDate": "2025-10-03T18:30:00.000",
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
                .andExpect(jsonPath("$.data.startDate").value("2025-10-03T17:00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-10-03T18:30:00"))
                .andExpect(jsonPath("$.data.repeatRule").doesNotExist());

    }

    @Test
    @DisplayName("단발성 계획 생성 - 밀리초까지도 둘 다 전송받는 경우")
    void t1_1() throws Exception {

        ResultActions resultActions = mvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "단발성 계획 - 밀리초 포함",
                        "startDate": "2025-09-21T05:00:00.000",
                        "endDate": "2025-09-21T07:00:00.000",
                        "color": "RED"
                    }
                    """))
                .andDo(print());

        resultActions
                .andExpect(status().isOk()) // 200 OK인지 확인
                .andExpect(handler().handlerType(StudyPlanController.class))
                .andExpect(handler().methodName("createStudyPlan"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("학습 계획이 성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.startDate").value("2025-09-21T05:00:00"))
                .andExpect(jsonPath("$.data.endDate").value("2025-09-21T07:00:00"))
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

    // 수정 후 삭제 시나리오 테스트
    @Test
    @DisplayName("반복 계획 수정(FROM_THIS_DATE) 후 삭제(THIS_ONLY) 시 중복 예외 발생 방지 테스트")
    void t16() throws Exception {
        // Given: 매일 반복되는 계획 생성 (10월 1일 ~ 12월 31일)
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();

        // When 1: 10월 7일 계획을 FROM_THIS_DATE로 수정
        mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=FROM_THIS_DATE", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "수정된 반복 계획",
                        "startDate": "2025-10-07T14:00:00",
                        "endDate": "2025-10-07T15:00:00",
                        "color": "RED"
                    }
                    """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // When 2: 같은 날짜(10월 7일)를 THIS_ONLY로 삭제
        mvc.perform(MockMvcRequestBuilders.delete("/api/plans/{planId}?selectedDate=2025-10-07&applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedDate").value("2025-10-07"))
                .andExpect(jsonPath("$.data.applyScope").value("THIS_ONLY"));

        // Then: 10월 7일 조회 시 500 에러가 발생하지 않고 빈 결과 반환
        mvc.perform(get("/api/plans/date/2025-10-07")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.plans", hasSize(0)));

        // 10월 6일은 원본 그대로 조회되어야 함
        mvc.perform(get("/api/plans/date/2025-10-06")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.plans[0].subject").value("매일 반복 계획"));

        // 10월 8일은 수정된 내용으로 조회되어야 함
        mvc.perform(get("/api/plans/date/2025-10-08")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.plans[0].subject").value("수정된 반복 계획"));
    }

    @Test
    @DisplayName("반복 계획 수정(THIS_ONLY) 후 삭제(THIS_ONLY) 시에도 중복 예외 발생 방지")
    void t17() throws Exception {
        // Given: 매일 반복되는 계획 생성
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();

        // When 1: 10월 5일 계획을 THIS_ONLY로 수정
        mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "특정 날짜만 수정",
                        "startDate": "2025-10-05T16:00:00",
                        "endDate": "2025-10-05T17:00:00",
                        "color": "GREEN"
                    }
                    """))
                .andDo(print())
                .andExpect(status().isOk());

        // When 2: 같은 날짜를 THIS_ONLY로 삭제
        mvc.perform(MockMvcRequestBuilders.delete("/api/plans/{planId}?selectedDate=2025-10-05&applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Then: 10월 5일 조회 시 정상 동작
        mvc.perform(get("/api/plans/date/2025-10-05")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));

        // 다른 날짜들은 영향받지 않아야 함
        mvc.perform(get("/api/plans/date/2025-10-06")
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.plans[0].subject").value("매일 반복 계획"));
    }

    @Test
    @DisplayName("다음 날에 이미 예외가 있을 때 중복 생성 방지 테스트")
    void t18() throws Exception {
        // Given: 매일 반복되는 계획 생성
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();

        // When 1: 10월 7일을 FROM_THIS_DATE로 수정
        mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=FROM_THIS_DATE", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "10월 7일부터 수정",
                        "startDate": "2025-10-07T14:00:00",
                        "endDate": "2025-10-07T15:00:00",
                        "color": "RED"
                    }
                    """))
                .andDo(print())
                .andExpect(status().isOk());

        // When 2: 10월 8일을 THIS_ONLY로 별도 수정 (다른 내용)
        mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "10월 8일만 특별 수정",
                        "startDate": "2025-10-08T16:00:00",
                        "endDate": "2025-10-08T17:00:00",
                        "color": "GREEN"
                    }
                    """))
                .andDo(print())
                .andExpect(status().isOk());

        // When 3: 10월 7일을 THIS_ONLY로 삭제
        mvc.perform(MockMvcRequestBuilders.delete("/api/plans/{planId}?selectedDate=2025-10-07&applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print())
                .andExpect(status().isOk());

        // Then 1: 10월 7일은 삭제됨
        mvc.perform(get("/api/plans/date/2025-10-07")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));

        // Then 2: 10월 8일은 기존에 설정한 "10월 8일만 특별 수정" 유지 (중복 생성되지 않음)
        mvc.perform(get("/api/plans/date/2025-10-08")
                        .header("Authorization", "Bearer faketoken"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.plans[0].subject").value("10월 8일만 특별 수정"))
                .andExpect(jsonPath("$.data.plans[0].color").value("GREEN"))
                .andExpect(jsonPath("$.data.plans[0].startDate").value("2025-10-08T16:00:00"));

        // Then 3: DB에서 10월 8일 예외가 1개만 존재하는지 확인
        List<StudyPlanException> exceptions = studyPlanExceptionRepository.findAll();
        long oct8Exceptions = exceptions.stream()
                .filter(e -> e.getExceptionDate().equals(LocalDate.of(2025, 10, 8)))
                .count();
        assertThat(oct8Exceptions).isEqualTo(1);
    }

    @Test
    @DisplayName("버그 수정: 10.5 일괄 수정 → 10.6 단일 수정 → 10.5 단일 삭제 시 10.7~10.10 수정 유지")
    void testBugFix_ModificationContinuesAfterSkippingException() throws Exception {
        // Given: 매일 반복되는 계획 생성 (10.1~10.10)
        StudyPlan originalPlan = createDailyPlan();
        Long planId = originalPlan.getId();

        // When 1: 10.5 FROM_THIS_DATE 수정
        mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=FROM_THIS_DATE", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "빨간색",
                        "startDate": "2025-10-05T14:00:00",
                        "endDate": "2025-10-05T15:00:00",
                        "color": "RED"
                    }
                    """))
                .andExpect(status().isOk());

        // When 2: 10.6 THIS_ONLY 수정
        mvc.perform(MockMvcRequestBuilders.put("/api/plans/{planId}?applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "subject": "초록색",
                        "startDate": "2025-10-06T16:00:00",
                        "endDate": "2025-10-06T17:00:00",
                        "color": "GREEN"
                    }
                    """))
                .andExpect(status().isOk());

        // When 3: 10.5 THIS_ONLY 삭제
        mvc.perform(MockMvcRequestBuilders.delete("/api/plans/{planId}?selectedDate=2025-10-05&applyScope=THIS_ONLY", planId)
                        .header("Authorization", "Bearer faketoken"))
                .andExpect(status().isOk());

        // Then: 10.5 삭제됨
        mvc.perform(get("/api/plans/date/2025-10-05")
                        .header("Authorization", "Bearer faketoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));

        // Then: 10.6 초록색 유지
        mvc.perform(get("/api/plans/date/2025-10-06")
                        .header("Authorization", "Bearer faketoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plans[0].subject").value("초록색"));

        // Then: 10.7~10.10 빨간색 유지 (버그 수정 확인)
        mvc.perform(get("/api/plans/date/2025-10-07")
                        .header("Authorization", "Bearer faketoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plans[0].subject").value("빨간색"));

        mvc.perform(get("/api/plans/date/2025-10-10")
                        .header("Authorization", "Bearer faketoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plans[0].subject").value("빨간색"));
    }

}