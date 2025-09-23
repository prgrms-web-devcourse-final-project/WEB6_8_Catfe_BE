package com.back.domain.user.repository;

import com.back.domain.user.entity.PrivateChatMessage;
import com.back.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrivateChatMessageRepository extends JpaRepository<PrivateChatMessage, Long> {

    // 두 사용자 간의 페이징된 대화 메시지 조회 (무한 스크롤용)
    @Query("SELECT m FROM PrivateChatMessage m " +
            "WHERE (m.fromUser.id = :userId1 AND m.toUser.id = :userId2) " +
            "OR (m.fromUser.id = :userId2 AND m.toUser.id = :userId1) " +
            "ORDER BY m.createdAt DESC")
    Page<PrivateChatMessage> findConversationBetweenUsers(@Param("userId1") Long userId1,
                                                          @Param("userId2") Long userId2,
                                                          Pageable pageable);

    // 두 사용자 간의 페이징된 대화 메시지 조회 (무한 스크롤용) - 최신 메시지부터
    @Query("SELECT m FROM PrivateChatMessage m " +
            "WHERE ((m.fromUser.id = :userId1 AND m.toUser.id = :userId2) " +
            "OR (m.fromUser.id = :userId2 AND m.toUser.id = :userId1)) " +
            "AND m.createdAt > :timestamp " +
            "ORDER BY m.createdAt ASC")
    List<PrivateChatMessage> findNewMessagesBetweenUsers(@Param("userId1") Long userId1,
                                                         @Param("userId2") Long userId2,
                                                         @Param("timestamp") LocalDateTime timestamp);

    // 두 사용자 간의 최근 20개 메시지 조회 (초기 로드용)
    @Query("SELECT DISTINCT " +
            "CASE WHEN m.fromUser.id = :userId THEN m.toUser ELSE m.fromUser END " +
            "FROM PrivateChatMessage m " +
            "WHERE m.fromUser.id = :userId OR m.toUser.id = :userId")
    List<User> findConversationPartners(@Param("userId") Long userId);

    // 두 사용자 간의 최신 메시지 조회
    @Query("SELECT m FROM PrivateChatMessage m " +
            "WHERE (m.fromUser.id = :userId1 AND m.toUser.id = :userId2) " +
            "OR (m.fromUser.id = :userId2 AND m.toUser.id = :userId1) " +
            "ORDER BY m.createdAt DESC " +
            "LIMIT 1")
    PrivateChatMessage findLatestMessageBetweenUsers(@Param("userId1") Long userId1,
                                                     @Param("userId2") Long userId2);

    // 두 사용자 간의 전체 메시지 수 조회
    @Query("SELECT COUNT(m) FROM PrivateChatMessage m " +
            "WHERE (m.fromUser.id = :userId1 AND m.toUser.id = :userId2) " +
            "OR (m.fromUser.id = :userId2 AND m.toUser.id = :userId1)")
    long countMessagesBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}