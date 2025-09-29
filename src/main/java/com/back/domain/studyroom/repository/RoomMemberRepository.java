package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 현재.. )
 findByConnectionId 메서드 제거 (connectionId 필드 제거로 인해)
 실시간 상태 관련 쿼리 메서드 제거
 커스텀 쿼리는 RoomMemberRepositoryCustom에서 관리
 */
@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long>, RoomMemberRepositoryCustom {
    // 기본 CRUD는 JpaRepository가 제공
    // 커스텀 쿼리는 RoomMemberRepositoryCustom에 정의
}
