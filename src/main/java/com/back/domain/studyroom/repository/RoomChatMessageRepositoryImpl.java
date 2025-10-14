package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.QRoom;
import com.back.domain.studyroom.entity.QRoomChatMessage;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.user.common.entity.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;



@Repository
@RequiredArgsConstructor
public class RoomChatMessageRepositoryImpl implements RoomChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QRoomChatMessage message = QRoomChatMessage.roomChatMessage;
    private final QRoom room = QRoom.room;
    private final QUser user = QUser.user;

    @Override
    public Page<RoomChatMessage> findMessagesByRoomId(Long roomId, Pageable pageable) {

        // 메시지 목록 조회
        List<RoomChatMessage> messages = queryFactory
                .selectFrom(message)
                .leftJoin(message.room, room).fetchJoin()  // Room 정보 즉시 로딩
                .leftJoin(message.user, user).fetchJoin()  // User 정보 즉시 로딩
                .leftJoin(user.userProfile).fetchJoin() // UserProfile 정보 즉시 로딩
                .where(message.room.id.eq(roomId))
                .orderBy(message.createdAt.desc()) // 최신순 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long totalCount = queryFactory
                .select(message.count())
                .from(message)
                .where(message.room.id.eq(roomId))
                .fetchOne();

        return new PageImpl<>(messages, pageable, totalCount != null ? totalCount : 0);
    }

    @Override
    public Page<RoomChatMessage> findMessagesByRoomIdBefore(Long roomId, LocalDateTime before, Pageable pageable) {

        // 조건부 WHERE 절 (before가 null이면 조건 제외)
        BooleanExpression whereClause = message.room.id.eq(roomId);
        if (before != null) {
            whereClause = whereClause.and(message.createdAt.lt(before)); // before 시점 이전
        }

        List<RoomChatMessage> messages = queryFactory
                .selectFrom(message)
                .leftJoin(message.room, room).fetchJoin()
                .leftJoin(message.user, user).fetchJoin()
                .leftJoin(user.userProfile).fetchJoin()
                .where(whereClause)
                .orderBy(message.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 조건에 맞는 전체 개수
        Long totalCount = queryFactory
                .select(message.count())
                .from(message)
                .where(whereClause)
                .fetchOne();

        return new PageImpl<>(messages, pageable, totalCount != null ? totalCount : 0);
    }

    @Override
    public int deleteAllMessagesByRoomId(Long roomId) {
        return Math.toIntExact(queryFactory
                .delete(message)
                .where(message.room.id.eq(roomId))
                .execute());
    }

}
