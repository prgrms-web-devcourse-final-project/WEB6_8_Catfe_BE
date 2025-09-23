package com.back.domain.study.plan.repository;

import com.back.domain.study.plan.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Integer> {

}
