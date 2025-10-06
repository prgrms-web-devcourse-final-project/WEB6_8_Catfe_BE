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
     * 방의 온라인 멤버 조회
     * TODO: Redis 기반으로 변경 예정
     * 현재는 방의 모든 멤버 반환 (임시)
     * @param roomId 방 ID
     * @return 멤버 목록 (역할순, 입장순 정렬)
     */
    @Override
    @Deprecated
    public List<RoomMember> findOnlineMembersByRoomId(Long roomId) {
        return queryFactory
                .selectFrom(roomMember)
                .leftJoin(roomMember.user, user).fetchJoin()  // N+1 방지
                .where(roomMember.room.id.eq(roomId))
                .orderBy(
                        roomMember.role.asc(),       // 역할순
                        roomMember.joinedAt.asc()    // 입장 시간순
                )
                .fetch();
    }

    /**
     * 방의 활성 멤버 수 조회
     * TODO: Redis 기반으로 변경 예정
     * 현재는 방의 모든 멤버 수 반환 (임시)
     * @param roomId 방 ID
     * @return 멤버 수
     */
    @Override
    @Deprecated
    public int countActiveMembersByRoomId(Long roomId) {
        Long count = queryFactory
                .select(roomMember.count())
                .from(roomMember)
                .where(roomMember.room.id.eq(roomId))
                .fetchOne();

        return count != null ? count.intValue() : 0;
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
     * 예시:
     * ```java
     * // 방의 모든 부방장 조회
     * List<RoomMember> subHosts = findByRoomIdAndRole(roomId, RoomRole.SUB_HOST);
     * ```
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
     * - 정상적인 방이라면 반드시 방장이 1명 존재
     * - Optional.empty()인 경우는 데이터 오류 상태
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
     * - HOST: 방장 (최고 권한)
     * - SUB_HOST: 부방장 (방장이 위임한 권한)
     * - 관리자 목록 표시
     * - 권한 체크 (이 목록에 있는 사용자만 특정 작업 가능)
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
     * - HOST 또는 SUB_HOST 역할
     * - 방 설정 변경 권한 체크
     * - 멤버 추방 권한 체크
     * - 공지사항 작성 권한 체크
     * 사용 예시:
     * ```java
     * if (!roomMemberRepository.isManager(roomId, userId)) {
     *     throw new CustomException(ErrorCode.NOT_ROOM_MANAGER);
     * }
     * ```
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
     * ( 해당 로직 활용해서 유저 밴 등으로 추후에 확장 가능)
     * - 방 입장 전 중복 참여 체크
     * - 비공개 방 접근 권한 확인
     * - 멤버 전용 기능 접근 권한 확인
     * 사용 예시:
     * ```java
     * if (room.isPrivate() && !roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
     *     throw new CustomException(ErrorCode.ROOM_FORBIDDEN);
     * }
     * ```
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
     * 특정 역할의 멤버 수 조회
     * TODO: Redis 기반으로 변경 예정
     * @param roomId 방 ID
     * @param role 역할
     * @return 해당 역할의 멤버 수
     */
    @Override
    @Deprecated
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

    /**
     * 방 퇴장 처리 (벌크 업데이트)
     * TODO: Redis로 이관 예정
     * 현재는 아무 동작 안함 (DB에는 멤버십 유지)
     * @param roomId 방 ID
     * @param userId 사용자 ID
     */
    @Override
    @Deprecated
    public void leaveRoom(Long roomId, Long userId) {
        // Redis로 이관 예정 - 현재는 아무 동작 안함
        // DB의 멤버십은 유지됨
    }

    /**
     * 방의 모든 멤버를 오프라인 처리 (방 종료 시)
     * TODO: Redis로 이관 예정
     * 현재는 아무 동작 안함
     * @param roomId 방 ID
     */
    @Override
    @Deprecated
    public void disconnectAllMembers(Long roomId) {
        // Redis로 이관 예정 - 현재는 아무 동작 안함
    }

    /**
     * 스터디룸의 모든 멤버 User ID 조회 (알림 전송용)
     * - 알림 대상자 조회
     * - N+1 방지를 위해 User ID만 조회
     * @param roomId 방 ID
     * @return 멤버들의 User ID 목록
     */
    @Override
    public List<Long> findUserIdsByRoomId(Long roomId) {
        return queryFactory
                .select(roomMember.user.id)
                .from(roomMember)
                .where(roomMember.room.id.eq(roomId))
                .fetch();
    }
}
