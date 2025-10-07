package com.back.domain.notification.event.study;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DailyGoalAchievedEvent extends StudyNotificationEvent {
    private final LocalDate achievedDate;
    private final int completedPlans;
    private final int totalPlans;

    public DailyGoalAchievedEvent(Long userId, LocalDate achievedDate,
                                  int completedPlans, int totalPlans) {
        super(
                userId,
                "일일 목표 달성 🎉",
                String.format("오늘의 학습 계획을 모두 완료했습니다! (%d/%d)",
                        completedPlans, totalPlans)
        );
        this.achievedDate = achievedDate;
        this.completedPlans = completedPlans;
        this.totalPlans = totalPlans;
    }
}
