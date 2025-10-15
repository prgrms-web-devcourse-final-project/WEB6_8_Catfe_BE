package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomGuestbookReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomGuestbookReactionRepository extends JpaRepository<RoomGuestbookReaction, Long> {

    /**
     * 특정 방명록의 모든 반응 조회
     */
    @Query("SELECT r FROM RoomGuestbookReaction r " +
           "JOIN FETCH r.user " +
           "WHERE r.guestbook.id = :guestbookId")
    List<RoomGuestbookReaction> findByGuestbookIdWithUser(@Param("guestbookId") Long guestbookId);

    /**
     * 특정 방명록에 특정 사용자가 특정 이모지로 반응했는지 조회
     */
    Optional<RoomGuestbookReaction> findByGuestbookIdAndUserIdAndEmoji(
            Long guestbookId, Long userId, String emoji);

    /**
     * 특정 방명록에 특정 사용자의 모든 반응 조회
     */
    List<RoomGuestbookReaction> findByGuestbookIdAndUserId(Long guestbookId, Long userId);

    /**
     * 특정 방명록의 특정 이모지 반응 개수
     */
    long countByGuestbookIdAndEmoji(Long guestbookId, String emoji);

    /**
     * 특정 방명록의 전체 반응 개수
     */
    long countByGuestbookId(Long guestbookId);
}
