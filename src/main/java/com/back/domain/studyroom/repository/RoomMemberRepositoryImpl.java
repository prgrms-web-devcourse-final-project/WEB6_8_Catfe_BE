package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.QRoomMember;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.entity.RoomRole;
import com.back.domain.user.entity.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 방별/사용자별 멤버십 조회
 isOnline, connectionId 필드 제거
 실시간 상태 관련 메서드 제거 (Redis로 이관)
 벌크 업데이트 쿼리 제거 (불필요)
 */
@Repository
@RequiredArgsConstructor
public class RoomMemberRepositoryImpl implements RoomMemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QRoomMember roomMember = QRoomMember.roomMember;
    private final QUser user = QUser.user;

    /**
     * 방의 특정 사용자 멤버십 조회
     */
    @Override
    public Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId) {
        RoomMember member = queryFactory
                .selectFrom(roomMember)
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.user.id.eq(userId)
                )
                .fetchOne();

        return Optional.ofNullable(member);
    }

    /**
     * 방의 모든 멤버 조회 (역할순 정렬)
     * 정렬 우선순위:
     1. 역할 (HOST > SUB_HOST > MEMBER > VISITOR)
     2. 가입 시간 (먼저 가입한 순)
     */
    @Override
    public List<RoomMember> findByRoomIdOrderByRole(Long roomId) {
        return queryFactory
                .selectFrom(roomMember)
                .leftJoin(roomMember.user, user).fetchJoin()  // N+1 방지
                .where(roomMember.room.id.eq(roomId))
                .orderBy(
                        roomMember.role.asc(),      // 역할순 (HOST가 먼저)
                        roomMember.joinedAt.asc()   // 가입 시간순
                )
                .fetch();
    }

    /**
     방의 멤버 중 특정 사용자 ID 목록에 해당하는 멤버만 조회
     * 현재 음 구현한 시나리오 로직:
     1. WebSocketSessionManager에서 온라인 사용자 ID 목록 조회 (Redis)
     2. 이 메서드로 해당 ID들의 상세 멤버 정보 조회 (DB)
     3. RoomMemberResponse DTO로 변환하여 클라이언트에 반환
     * @param roomId 방 ID
     * @param userIds 조회할 사용자 ID 목록 (Redis에서 가져온 온라인 사용자)
     * @return 해당하는 멤버 목록 (역할순 정렬)
     */
    @Override
    public List<RoomMember> findByRoomIdAndUserIdIn(Long roomId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(roomMember)
                .leftJoin(roomMember.user, user).fetchJoin()  // N+1 방지
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.user.id.in(userIds)
                )
                .orderBy(
                        roomMember.role.asc(),              // 역할순
                        roomMember.lastActiveAt.desc()      // 최근 활동순
                )
                .fetch();
    }

    /**
     * 특정 역할의 멤버 조회
     */
    @Override
    public List<RoomMember> findByRoomIdAndRole(Long roomId, RoomRole role) {
        return queryFactory
                .selectFrom(roomMember)
                .leftJoin(roomMember.user, user).fetchJoin()
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.role.eq(role)
                )
                .fetch();
    }

    /**
     * 방장 조회
     */
    @Override
    public Optional<RoomMember> findHostByRoomId(Long roomId) {
        RoomMember host = queryFactory
                .selectFrom(roomMember)
                .leftJoin(roomMember.user, user).fetchJoin()
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.role.eq(RoomRole.HOST)
                )
                .fetchOne();

        return Optional.ofNullable(host);
    }

    /**
     * 관리자 권한을 가진 멤버들 조회 (HOST, SUB_HOST)
     */
    @Override
    public List<RoomMember> findManagersByRoomId(Long roomId) {
        return queryFactory
                .selectFrom(roomMember)
                .leftJoin(roomMember.user, user).fetchJoin()
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.role.in(RoomRole.HOST, RoomRole.SUB_HOST)
                )
                .orderBy(roomMember.role.asc())  // HOST가 먼저
                .fetch();
    }

    /**
     * 사용자가 특정 방에서 관리자 권한을 가지고 있는지 확인
     */
    @Override
    public boolean isManager(Long roomId, Long userId) {
        Long count = queryFactory
                .select(roomMember.count())
                .from(roomMember)
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.user.id.eq(userId),
                        roomMember.role.in(RoomRole.HOST, RoomRole.SUB_HOST)
                )
                .fetchOne();

        return count != null && count > 0;
    }

    /**
     * 사용자가 이미 해당 방의 멤버인지 확인
     */
    @Override
    public boolean existsByRoomIdAndUserId(Long roomId, Long userId) {
        Long count = queryFactory
                .select(roomMember.count())
                .from(roomMember)
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.user.id.eq(userId)
                )
                .fetchOne();

        return count != null && count > 0;
    }

    /**
     * 특정 역할의 멤버 수 조회
     */
    @Override
    public int countByRoomIdAndRole(Long roomId, RoomRole role) {
        Long count = queryFactory
                .select(roomMember.count())
                .from(roomMember)
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.role.eq(role)
                )
                .fetchOne();

        return count != null ? count.intValue() : 0;
    }
}
