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

/**
 * 주요 기능:
 * - 방별/사용자별 멤버십 조회
 * - 역할(Role)별 멤버 필터링
 * - 온라인 상태 관리
 * - JOIN FETCH를 통한 N+1 문제 해결
 * - 벌크 업데이트 쿼리
 */
@Repository
@RequiredArgsConstructor
public class RoomMemberRepositoryImpl implements RoomMemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // QueryDSL Q 클래스 인스턴스
    private final QRoomMember roomMember = QRoomMember.roomMember;
    private final QUser user = QUser.user;

    /**
     * 방의 특정 사용자 멤버십 조회
     * - 사용자가 특정 방에 참여 중인지 확인
     * - 방 입장 시 기존 멤버십 존재 여부 확인
     * - 사용자의 방 내 역할 확인
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @return 멤버십 정보 (Optional)
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
     * - 1순위: 역할 (HOST > SUB_HOST > MEMBER > VISITOR)
     * - 2순위: 입장 시간 (먼저 입장한 순)
     * - 방 설정 페이지에서 전체 멤버 목록 표시
     * - 멤버 관리 기능
     * @param roomId 방 ID
     * @return 정렬된 멤버 목록
     */
    @Override
    public List<RoomMember> findByRoomIdOrderByRole(Long roomId) {
        return queryFactory
                .selectFrom(roomMember)
                .where(roomMember.room.id.eq(roomId))
                .orderBy(
                        roomMember.role.asc(),      // 역할순 (HOST가 먼저)
                        roomMember.joinedAt.asc()   // 입장 시간순
                )
                .fetch();
    }

    /**
     * 여러 사용자의 멤버십 일괄 조회 (IN 절)
     * - Redis 온라인 목록으로 DB 멤버십 조회
     * - N+1 문제 해결
     * - VISITOR는 DB에 없으므로 결과에 포함 안됨
     * @param roomId 방 ID
     * @param userIds 사용자 ID Set
     * @return DB에 저장된 멤버 목록 (MEMBER 이상)
     */
    @Override
    public List<RoomMember> findByRoomIdAndUserIdIn(Long roomId, java.util.Set<Long> userIds) {
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
                        roomMember.role.asc(),       // 역할순
                        roomMember.joinedAt.asc()    // 입장 시간순
                )
                .fetch();
    }

    /**
     * 사용자가 참여 중인 모든 방의 멤버십 조회
     * DB에 저장된 멤버십만 조회 (MEMBER 이상)
     * @param userId 사용자 ID
     * @return 멤버십 목록
     */
    @Override
    public List<RoomMember> findActiveByUserId(Long userId) {
        return queryFactory
                .selectFrom(roomMember)
                .where(roomMember.user.id.eq(userId))
                .fetch();
    }

    /**
     * 특정 역할의 멤버 조회
     * - 방장(HOST) 찾기
     * - 부방장(SUB_HOST) 목록 조회
     * - 역할별 멤버 필터링
     * @param roomId 방 ID
     * @param role 역할 (HOST, SUB_HOST, MEMBER, VISITOR)
     * @return 해당 역할의 멤버 목록
     */
    @Override
    public List<RoomMember> findByRoomIdAndRole(Long roomId, RoomRole role) {
        return queryFactory
                .selectFrom(roomMember)
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.role.eq(role)
                )
                .fetch();
    }

    /**
     * 방장 조회
     * - 방장 권한 확인
     * - 방 소유자 정보 표시
     * @param roomId 방 ID
     * @return 방장 멤버십 (Optional)
     */
    @Override
    public Optional<RoomMember> findHostByRoomId(Long roomId) {
        RoomMember host = queryFactory
                .selectFrom(roomMember)
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.role.eq(RoomRole.HOST)
                )
                .fetchOne();

        return Optional.ofNullable(host);
    }

    /**
     * 관리자 권한을 가진 멤버들 조회 (HOST, SUB_HOST)
     * @param roomId 방 ID
     * @return 관리자 멤버 목록 (HOST, SUB_HOST)
     */
    @Override
    public List<RoomMember> findManagersByRoomId(Long roomId) {
        return queryFactory
                .selectFrom(roomMember)
                .where(
                        roomMember.room.id.eq(roomId),
                        roomMember.role.in(RoomRole.HOST, RoomRole.SUB_HOST)
                )
                .orderBy(roomMember.role.asc())  // HOST가 먼저
                .fetch();
    }

    /**
     * 사용자가 특정 방에서 관리자 권한을 가지고 있는지 확인
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @return 관리자 권한 여부
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
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @return 멤버 여부
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
}
