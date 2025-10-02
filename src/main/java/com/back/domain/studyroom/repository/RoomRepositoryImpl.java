package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.QRoom;
import com.back.domain.studyroom.entity.QRoomMember;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomStatus;
import com.back.domain.user.entity.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomRepositoryImpl implements RoomRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    // QueryDSL Q 클래스 인스턴스
    private final QRoom room = QRoom.room;
    private final QRoomMember roomMember = QRoomMember.roomMember;
    private final QUser user = QUser.user;

    /**
     * 공개 방 중 입장 가능한 방들 조회 (페이징)
     * 조회 조건:
     * - 비공개가 아닌 방 (isPrivate = false)
     * - 활성화된 방 (isActive = true)
     * - 입장 가능한 상태 (WAITING 또는 ACTIVE)
     * - 정원이 가득 차지 않은 방
     * @param pageable 페이징 정보
     * @return 페이징된 방 목록
     */
    @Override
    public Page<Room> findJoinablePublicRooms(Pageable pageable) {
        // 방 목록 조회
        List<Room> rooms = queryFactory
                .selectFrom(room)
                .leftJoin(room.createdBy, user).fetchJoin()  // N+1 방지
                .where(
                        room.isPrivate.eq(false),
                        room.isActive.eq(true),
                        room.status.in(RoomStatus.WAITING, RoomStatus.ACTIVE),
                        room.currentParticipants.lt(room.maxParticipants)
                )
                .orderBy(room.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회 (페이징 정보 생성용)
        Long totalCount = queryFactory
                .select(room.count())
                .from(room)
                .where(
                        room.isPrivate.eq(false),
                        room.isActive.eq(true),
                        room.status.in(RoomStatus.WAITING, RoomStatus.ACTIVE),
                        room.currentParticipants.lt(room.maxParticipants)
                )
                .fetchOne();

        return new PageImpl<>(rooms, pageable, totalCount != null ? totalCount : 0);
    }

    /**
     * 사용자가 참여 중인 방 조회
     * 조회 조건:
     * - 특정 사용자가 멤버로 등록된 방 (DB에 저장된 멤버십)
     * TODO: Redis에서 온라인 상태 확인하도록 변경
     * @param userId 사용자 ID
     * @return 참여 중인 방 목록
     */
    @Override
    public List<Room> findRoomsByUserId(Long userId) {
        return queryFactory
                .selectFrom(room)
                .leftJoin(room.createdBy, user).fetchJoin()  // N+1 방지
                .join(room.roomMembers, roomMember)          // 멤버 조인
                .where(roomMember.user.id.eq(userId))
                .fetch();
    }

    /**
     * 제목과 상태로 방 검색 (동적 쿼리)
     * 검색 조건 (모두 선택적):
     * - title: 방 제목에 포함된 문자열 (대소문자 무시)
     * - status: 방 상태 (WAITING, ACTIVE, PAUSED, TERMINATED)
     * - isPrivate: 공개/비공개 여부
     * 사용 예시:
     * - 제목만: "스터디" 포함된 모든 방
     * - 상태만: ACTIVE 상태인 모든 방
     * - 조합: "자바" 포함 + ACTIVE + 공개방
     * @param title 검색할 제목 (null 가능)
     * @param status 방 상태 (null 가능)
     * @param isPrivate 공개/비공개 (null 가능)
     * @param pageable 페이징 정보
     * @return 페이징된 검색 결과
     */
    @Override
    public Page<Room> findRoomsWithFilters(String title, RoomStatus status, Boolean isPrivate, Pageable pageable) {
        BooleanExpression whereClause = null;

        // 제목 검색 조건 (대소문자 무시)
        if (title != null && !title.isEmpty()) {
            whereClause = room.title.containsIgnoreCase(title);
        }

        // 상태 조건 추가
        if (status != null) {
            whereClause = whereClause != null 
                    ? whereClause.and(room.status.eq(status)) 
                    : room.status.eq(status);
        }

        // 공개/비공개 조건 추가
        if (isPrivate != null) {
            whereClause = whereClause != null 
                    ? whereClause.and(room.isPrivate.eq(isPrivate)) 
                    : room.isPrivate.eq(isPrivate);
        }

        // 쿼리 빌더 생성
        JPAQuery<Room> query = queryFactory
                .selectFrom(room)
                .leftJoin(room.createdBy, user).fetchJoin();  // N+1 방지

        // 동적으로 WHERE 절 추가
        if (whereClause != null) {
            query.where(whereClause);
        }

        // 방 목록 조회
        List<Room> rooms = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long totalCount = queryFactory
                .select(room.count())
                .from(room)
                .where(whereClause)
                .fetchOne();

        return new PageImpl<>(rooms, pageable, totalCount != null ? totalCount : 0);
    }

    /**
     * 인기 방 조회 (참가자 수 기준)
     * 조회 조건:
     * - 공개 방만 (isPrivate = false)
     * - 활성화된 방만 (isActive = true)
     * @param pageable 페이징 정보
     * @return 페이징된 인기 방 목록
     */
    @Override
    public Page<Room> findPopularRooms(Pageable pageable) {
        List<Room> rooms = queryFactory
                .selectFrom(room)
                .leftJoin(room.createdBy, user).fetchJoin()  // N+1 방지
                .where(
                        room.isPrivate.eq(false),
                        room.isActive.eq(true)
                )
                .orderBy(
                        room.currentParticipants.desc(),  // 참가자 수 많은 순
                        room.createdAt.desc()             // 최신순
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long totalCount = queryFactory
                .select(room.count())
                .from(room)
                .where(
                        room.isPrivate.eq(false),
                        room.isActive.eq(true)
                )
                .fetchOne();

        return new PageImpl<>(rooms, pageable, totalCount != null ? totalCount : 0);
    }

    /**
     * 비활성 방 정리 (배치 작업용)
     * 대상:
     * - 참가자가 0명인 방
     * - ACTIVE 상태인 방
     * - cutoffTime 이전에 마지막으로 업데이트된 방
     * 처리:
     * - 상태를 TERMINATED로 변경
     * - isActive를 false로 변경
     * 사용 예시:
     * ```
     * // 1시간 이상 비어있는 방 정리
     * LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
     * int count = terminateInactiveRooms(cutoff);
     * log.info("정리된 방 개수: {}", count);
     * ```
     * @param cutoffTime 기준 시간 (이 시간 이전에 업데이트된 방 정리)
     * @return 정리된 방 개수
     */
    @Override
    public int terminateInactiveRooms(LocalDateTime cutoffTime) {
        long affectedRows = queryFactory
                .update(room)
                .set(room.status, RoomStatus.TERMINATED)
                .set(room.isActive, false)
                .where(
                        room.currentParticipants.eq(0),
                        room.status.eq(RoomStatus.ACTIVE),
                        room.updatedAt.lt(cutoffTime)
                )
                .execute();

        return (int) affectedRows;
    }

    /**
     * 비관적 락으로 방 조회 (동시성 제어용)
     * 동시성 제어:
     * - PESSIMISTIC_WRITE 락 사용
     * - 트랜잭션 종료 시까지 다른 트랜잭션의 읽기/쓰기 차단
     * 
     *  주의사항:
     * - 반드시 @Transactional 내에서 사용!!!
     * - 락 대기 시간이 길어질 수 있으므로 빠르게 처리
     * - 데드락 가능성 주의
     * 
     * 사용 예시:
     * ```java
     * @Transactional
     * public RoomMember joinRoom(Long roomId, Long userId) {
     *     Room room = roomRepository.findByIdWithLock(roomId)
     *         .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
     *     
     *     if (!room.canJoin()) {
     *         throw new CustomException(ErrorCode.ROOM_FULL);
     *     }
     *     
     *     room.incrementParticipant();
     *     // ...
     * }
     * ```
     * 
     * @param roomId 방 ID
     * @return 락이 걸린 방 (Optional)
     */
    @Override
    public Optional<Room> findByIdWithLock(Long roomId) {
        Room foundRoom = queryFactory
                .selectFrom(room)
                .where(room.id.eq(roomId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)  // 비관적 쓰기 락
                .fetchOne();

        return Optional.ofNullable(foundRoom);
    }
}
