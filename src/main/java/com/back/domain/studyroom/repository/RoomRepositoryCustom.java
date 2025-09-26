package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomRepositoryCustom {

    /**
     * 공개 방 중 입장 가능한 방들 조회 (페이징)
     * - 비공개가 아니고, 활성화되어 있고, 입장 가능한 상태이며, 정원이 가득 차지 않은 방
     * - JOIN FETCH로 N+1 문제 방지
     */
    Page<Room> findJoinablePublicRooms(Pageable pageable);

    /**
     * 사용자가 참여 중인 방 조회
     * - 해당 사용자가 멤버로 등록되어 있고 현재 온라인 상태인 방
     * - JOIN FETCH로 N+1 문제 방지
     */
    List<Room> findRoomsByUserId(Long userId);

    /**
     * 제목과 상태로 검색 (동적 쿼리)
     */
    Page<Room> findRoomsWithFilters(String title, RoomStatus status, Boolean isPrivate, Pageable pageable);

    /**
     * 인기 방 조회 (참가자 수 기준)
     * - JOIN FETCH로 N+1 문제 방지
     */
    Page<Room> findPopularRooms(Pageable pageable);

    /**
     * 비활성 방 정리 (배치용)
     * - 참가자가 0명이고 일정 시간 이상 비활성 상태인 방 종료
     * @return 종료된 방 개수
     */
    int terminateInactiveRooms(LocalDateTime cutoffTime);

    /**
     * 비관적 락으로 방 조회 (동시성 제어용)
     */
    Optional<Room> findByIdWithLock(Long roomId);
}
