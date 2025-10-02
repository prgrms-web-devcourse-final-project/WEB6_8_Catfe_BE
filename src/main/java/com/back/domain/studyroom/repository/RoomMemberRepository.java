package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long>, RoomMemberRepositoryCustom {
    // 모든 메서드는 RoomMemberRepositoryCustom 인터페이스로 이동
}
