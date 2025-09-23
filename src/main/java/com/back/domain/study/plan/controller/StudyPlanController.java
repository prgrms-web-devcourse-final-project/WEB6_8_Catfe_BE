package com.back.domain.study.plan.controller;

import com.back.domain.study.plan.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class StudyPlanController {
    private final StudyPlanService studyPlanService;


}
