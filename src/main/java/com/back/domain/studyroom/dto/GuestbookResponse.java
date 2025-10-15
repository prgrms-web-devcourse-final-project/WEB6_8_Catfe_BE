package com.back.domain.studyroom.dto;

import com.back.domain.studyroom.entity.RoomGuestbook;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GuestbookResponse {
    private Long guestbookId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isAuthor;  // 현재 사용자가 작성자인지
    private Boolean isPinned;  // 현재 사용자가 핀했는지
    private List<GuestbookReactionSummary> reactions;  // 이모지 반응 요약

    public static GuestbookResponse from(
            RoomGuestbook guestbook, 
            Long currentUserId, 
            List<GuestbookReactionSummary> reactions,
            boolean isPinned) {
        return GuestbookResponse.builder()
                .guestbookId(guestbook.getId())
                .userId(guestbook.getUser().getId())
                .nickname(guestbook.getUser().getNickname())
                .profileImageUrl(guestbook.getUser().getProfileImageUrl())
                .content(guestbook.getContent())
                .createdAt(guestbook.getCreatedAt())
                .updatedAt(guestbook.getUpdatedAt())
                .isAuthor(currentUserId != null && guestbook.isAuthor(currentUserId))
                .isPinned(isPinned)
                .reactions(reactions)
                .build();
    }
}
