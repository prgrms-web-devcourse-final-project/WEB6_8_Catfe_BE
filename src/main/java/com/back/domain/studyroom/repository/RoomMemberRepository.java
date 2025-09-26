package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long>, RoomMemberRepositoryCustom {
    /**
     * WebSocket 연결 ID로 멤버 조회
     */
    Optional<RoomMember> findByConnectionId(String connectionId);
}
