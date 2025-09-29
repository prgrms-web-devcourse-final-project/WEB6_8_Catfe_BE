package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface RoomChatMessageRepositoryCustom {

    /**
     * 방별 페이징된 채팅 메시지 조회 (최신순)
     * @param roomId 방 ID
     * @param pageable 페이징 정보
     * @return 페이징된 채팅 메시지 목록
     */
    Page<RoomChatMessage> findMessagesByRoomId(Long roomId, Pageable pageable);

    /**
     * 특정 시점 이전의 채팅 메시지 조회 (무한 스크롤용)
     * @param roomId 방 ID
     * @param before 기준 시점
     * @param pageable 페이징 정보
     * @return 기준 시점 이전의 메시지 목록
     */
    Page<RoomChatMessage> findMessagesByRoomIdBefore(Long roomId, LocalDateTime before, Pageable pageable);

    /**
     * 특정 방의 모든 채팅 메시지 삭제
     */
    int deleteAllMessagesByRoomId(Long roomId);
}
