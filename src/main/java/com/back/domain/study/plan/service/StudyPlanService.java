package com.back.domain.study.plan.service;

import com.back.domain.study.plan.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyPlanService{
    private final StudyPlanRepository studyPlanRepository;



}
