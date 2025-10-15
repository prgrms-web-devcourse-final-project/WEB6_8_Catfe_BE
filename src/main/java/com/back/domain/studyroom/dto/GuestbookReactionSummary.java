package com.back.domain.studyroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 방명록 이모지 반응 요약 정보
 * 이모지별 개수와 반응한 사용자 정보 포함
 */
@Getter
@Builder
@AllArgsConstructor
public class GuestbookReactionSummary {
    private String emoji;
    private Long count;
    private Boolean reactedByMe;  // 현재 사용자가 이 이모지로 반응했는지
    private List<String> recentUsers;  // 최근 반응한 사용자 닉네임 (최대 3명)
}
