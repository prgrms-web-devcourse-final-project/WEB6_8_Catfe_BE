package com.back.domain.study.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanDeleteRequest {
    private DeleteScope deleteScope;

    public enum DeleteScope {
        THIS_ONLY,      // 이 날짜만
        FROM_THIS_DATE  // 이 날짜부터 이후 모든 날짜
    }
}