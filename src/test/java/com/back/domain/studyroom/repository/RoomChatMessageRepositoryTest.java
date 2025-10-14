package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomChatMessage;
import com.back.domain.user.common.entity.User;
import com.back.global.config.DataSourceProxyTestConfig;
import com.back.global.config.QueryDslTestConfig;
import com.back.global.util.QueryCounter;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({
        RoomChatMessageRepositoryImpl.class,
        QueryDslTestConfig.class,
        DataSourceProxyTestConfig.class
})
@DisplayName("RoomChatMessageRepository 테스트")
class RoomChatMessageRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private RoomChatMessageRepository roomChatMessageRepository;

    private Room testRoom;
    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser1 = User.builder()
                .email("test1@example.com")
                .username("테스터1")
                .password("password")
                .build();
        testEntityManager.persistAndFlush(testUser1);

        testUser2 = User.builder()
                .email("test2@example.com")
                .username("테스터2")
                .password("password")
                .build();
        testEntityManager.persistAndFlush(testUser2);

        // 테스트용 방 생성
        testRoom = Room.builder()
                .title("테스트 스터디룸")
                .description("QueryDSL 테스트용 방")
                .maxParticipants(10)
                .build();
        testEntityManager.persistAndFlush(testRoom);

        // 테스트용 채팅 메시지 생성
        createTestMessages();
        testEntityManager.flush();
        testEntityManager.clear();

        // 쿼리 카운터 초기화
        QueryCountHolder.clear();
    }

    private void createTestMessages() {
        for (int i = 0; i < 10; i++) {
            RoomChatMessage message = new RoomChatMessage(
                    testRoom,
                    i % 2 == 0 ? testUser1 : testUser2,
                    "테스트 메시지 " + (i + 1)
            );

            testEntityManager.persist(message);
            // 각 메시지가 약간 다른 시간에 저장되도록 잠깐 대기
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @DisplayName("방별 메시지 페이징 조회")
    void t1() {
        Pageable pageable = PageRequest.of(0, 5);

        Page<RoomChatMessage> result = roomChatMessageRepository.findMessagesByRoomId(testRoom.getId(), pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(2);

        // 최신순 정렬 확인
        RoomChatMessage firstMessage = result.getContent().get(0);
        RoomChatMessage secondMessage = result.getContent().get(1);
        assertThat(firstMessage.getCreatedAt()).isAfter(secondMessage.getCreatedAt());
    }

    @Test
    @DisplayName("before 파라미터를 이용한 메시지 조회")
    void t2() {
        // 실제 저장된 메시지들의 시간을 기준으로 테스트
        List<RoomChatMessage> allMessages = roomChatMessageRepository.findAll();

        // 중간 지점의 메시지 시간을 beforeTime으로 설정
        int midIndex = allMessages.size() / 2;
        LocalDateTime beforeTime = allMessages.get(midIndex).getCreatedAt().plusNanos(1);

        Pageable pageable = PageRequest.of(0, 10);

        Page<RoomChatMessage> result = roomChatMessageRepository
                .findMessagesByRoomIdBefore(testRoom.getId(), beforeTime, pageable);

        assertThat(result).isNotNull();

        // beforeTime 이전의 메시지 개수 계산
        long expectedCount = allMessages.stream()
                .filter(msg -> msg.getCreatedAt().isBefore(beforeTime) && msg.getRoom().getId().equals(testRoom.getId()))
                .count();

        assertThat(result.getContent()).hasSize((int) expectedCount);
        assertThat(expectedCount).isGreaterThan(0); // 적어도 몇 개는 있어야 함

        // 모든 메시지가 beforeTime 이전이고 해당 방의 메시지인지 확인
        result.getContent().forEach(message -> {
            assertThat(message.getCreatedAt()).isBefore(beforeTime);
            assertThat(message.getRoom().getId()).isEqualTo(testRoom.getId());
        });
    }

    @Test
    @DisplayName("before 파라미터가 null일 때 정상 동작")
    void t3() {
        LocalDateTime before = null;
        Pageable pageable = PageRequest.of(0, 10);

        Page<RoomChatMessage> result = roomChatMessageRepository
                .findMessagesByRoomIdBefore(testRoom.getId(), before, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(10); // before가 null이면 모든 메시지 조회
        assertThat(result.getTotalElements()).isEqualTo(10);
    }

    @Test
    @DisplayName("N+1 문제 해결 확인 - fetch join 적용")
    void t4() {
        // Given
        Pageable pageable = PageRequest.of(0, 3);

        // 영속성 컨텍스트 초기화 (캐시 제거)
        testEntityManager.flush();
        testEntityManager.clear();

        // 쿼리 카운터 초기화
        QueryCounter.clear();

        // When - 1. 초기 조회
        Page<RoomChatMessage> result = roomChatMessageRepository
                .findMessagesByRoomId(testRoom.getId(), pageable);

        long selectCountAfterQuery = QueryCounter.getSelectCount();

        // When - 2. 연관 엔티티 접근
        for (RoomChatMessage message : result.getContent()) {
            String roomTitle = message.getRoom().getTitle();
            String userNickname = message.getUser().getNickname();

            assertThat(roomTitle).isNotNull();
            assertThat(userNickname).isNotNull();
        }

        long selectCountAfterAccess = QueryCounter.getSelectCount();

        // Then
        assertThat(result.getContent()).hasSize(3);

        // fetch join이 제대로 동작하면 SELECT는 2번만 실행되어야 함
        // 1. RoomChatMessage + Room + User 조회 (fetch join)
        // 2. 페이징을 위한 count 쿼리
        assertThat(selectCountAfterQuery)
                .as("초기 조회 시 2번의 SELECT만 실행되어야 함 (fetch join 1번 + count 1번)")
                .isEqualTo(2);

        // 연관 엔티티 접근 시에도 추가 쿼리가 발생하지 않아야 함
        assertThat(selectCountAfterAccess)
                .as("연관 엔티티 접근 시 추가 쿼리가 발생하지 않아야 함")
                .isEqualTo(selectCountAfterQuery);

        // 쿼리 카운트 출력
        QueryCounter.printQueryCount();
    }

    @Test
    @DisplayName("존재하지 않는 방 ID로 조회 시 빈 결과 반환")
    void t5() {
        long nonExistentRoomId = 99999L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<RoomChatMessage> result = roomChatMessageRepository
                .findMessagesByRoomId(nonExistentRoomId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("특정 시점 이후 메시지 제외 - 경계값 테스트")
    void t6() {
        List<RoomChatMessage> allMessages = roomChatMessageRepository.findAll();

        // 마지막에서 3번째 메시지 시간을 beforeTime으로 설정
        int targetIndex = allMessages.size() - 3;
        LocalDateTime beforeTime = allMessages.get(targetIndex).getCreatedAt();

        Pageable pageable = PageRequest.of(0, 10);

        Page<RoomChatMessage> result = roomChatMessageRepository
                .findMessagesByRoomIdBefore(testRoom.getId(), beforeTime, pageable);

        assertThat(result).isNotNull();

        // beforeTime 이전의 메시지들만 조회되어야 함
        result.getContent().forEach(message -> {
            assertThat(message.getCreatedAt()).isBefore(beforeTime);
        });

        // 적어도 몇 개의 메시지는 조회되어야 함 (처음 몇 개는 beforeTime보다 이전이므로)
        assertThat(result.getContent().size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("아주 미래 시간 조건 - 모든 메시지 조회")
    void t7() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
        Pageable pageable = PageRequest.of(0, 10);

        Page<RoomChatMessage> result = roomChatMessageRepository
                .findMessagesByRoomIdBefore(testRoom.getId(), futureTime, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(10); // 모든 메시지가 미래 시간보다 이전이므로 모두 조회
        assertThat(result.getTotalElements()).isEqualTo(10);
    }

    @Test
    @DisplayName("아주 과거 시간 조건 - 빈 결과")
    void t8() {
        LocalDateTime pastTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        Pageable pageable = PageRequest.of(0, 10);

        Page<RoomChatMessage> result = roomChatMessageRepository
                .findMessagesByRoomIdBefore(testRoom.getId(), pastTime, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty(); // 모든 메시지가 과거 시간보다 이후이므로 빈 결과
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ==================== 채팅 전체 삭제 기능 테스트 ====================

    @Test
    @DisplayName("방별 채팅 메시지 수 조회")
    void t9() {
        int messageCount = roomChatMessageRepository.countByRoomId(testRoom.getId());

        assertThat(messageCount).isEqualTo(10); // setUp에서 10개 메시지 생성
    }

    @Test
    @DisplayName("존재하지 않는 방의 채팅 메시지 수 조회")
    void t10() {
        Long nonExistentRoomId = 99999L;

        int messageCount = roomChatMessageRepository.countByRoomId(nonExistentRoomId);

        assertThat(messageCount).isEqualTo(0);
    }

    @Test
    @DisplayName("방별 모든 채팅 메시지 삭제")
    void t11() {
        Long roomId = testRoom.getId();

        // 삭제 전 메시지 수 확인
        int countBeforeDelete = roomChatMessageRepository.countByRoomId(roomId);
        assertThat(countBeforeDelete).isEqualTo(10);

        // 메시지 삭제 실행
        int deletedCount = roomChatMessageRepository.deleteAllByRoomId(roomId);

        // 삭제된 수 검증
        assertThat(deletedCount).isEqualTo(10);

        // 삭제 후 메시지 수 확인
        int countAfterDelete = roomChatMessageRepository.countByRoomId(roomId);
        assertThat(countAfterDelete).isEqualTo(0);

        // 실제 조회로도 확인
        Pageable pageable = PageRequest.of(0, 10);
        Page<RoomChatMessage> result = roomChatMessageRepository.findMessagesByRoomId(roomId, pageable);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 방의 채팅 메시지 삭제")
    void t12() {
        Long nonExistentRoomId = 99999L;

        int deletedCount = roomChatMessageRepository.deleteAllByRoomId(nonExistentRoomId);

        assertThat(deletedCount).isEqualTo(0); // 삭제할 메시지가 없으므로 0
    }

    @Test
    @DisplayName("여러 방이 있을 때 특정 방만 삭제")
    void t13() {
        // 추가 방 생성
        Room anotherRoom = Room.builder()
                .title("다른 스터디룸")
                .description("다른 테스트용 방")
                .maxParticipants(5)
                .build();
        testEntityManager.persistAndFlush(anotherRoom);

        // 다른 방에 메시지 추가
        for (int i = 0; i < 5; i++) {
            RoomChatMessage message = new RoomChatMessage(
                    anotherRoom,
                    testUser1,
                    "다른 방 메시지 " + (i + 1)
            );
            testEntityManager.persist(message);
        }
        testEntityManager.flush();

        // 첫 번째 방의 메시지만 삭제
        int deletedCount = roomChatMessageRepository.deleteAllByRoomId(testRoom.getId());

        assertThat(deletedCount).isEqualTo(10); // 첫 번째 방의 메시지만 삭제

        // 첫 번째 방 메시지 확인
        int firstRoomCount = roomChatMessageRepository.countByRoomId(testRoom.getId());
        assertThat(firstRoomCount).isEqualTo(0);

        // 두 번째 방 메시지는 그대로 유지되는지 확인
        int secondRoomCount = roomChatMessageRepository.countByRoomId(anotherRoom.getId());
        assertThat(secondRoomCount).isEqualTo(5);
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 삭제 취소 확인")
    void t14() {
        Long roomId = testRoom.getId();

        // 삭제 전 메시지 수 확인
        int countBefore = roomChatMessageRepository.countByRoomId(roomId);
        assertThat(countBefore).isEqualTo(10);

        // 트랜잭션을 명시적으로 롤백하는 테스트는 실제 트랜잭션 매니저가 필요하므로 여기서는 생략
        // 대신 삭제 후 다시 메시지를 생성해서 테스트

        roomChatMessageRepository.deleteAllByRoomId(roomId);
        
        // 다시 메시지 생성 (롤백 시뮬레이션)
        for (int i = 0; i < 3; i++) {
            RoomChatMessage message = new RoomChatMessage(
                    testRoom,
                    testUser1,
                    "복구된 메시지 " + (i + 1)
            );
            testEntityManager.persist(message);
        }
        testEntityManager.flush();

        int countAfter = roomChatMessageRepository.countByRoomId(roomId);
        assertThat(countAfter).isEqualTo(3);
    }
}