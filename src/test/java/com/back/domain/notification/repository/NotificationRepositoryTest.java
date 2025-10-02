package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationRead;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.entity.RoomMember;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.config.QueryDslTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslTestConfig.class)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationReadRepository notificationReadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private Room room1;
    private Room room2;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        user1 = User.builder()
                .email("user1@test.com")
                .username("유저1")
                .password("password123")
                .build();
        user2 = User.builder()
                .email("user2@test.com")
                .username("유저2")
                .password("password123")
                .build();
        userRepository.saveAll(List.of(user1, user2));

        // 테스트 스터디룸 생성
        room1 = Room.builder()
                .title("스터디룸1")
                .description("설명1")
                .createdBy(user1)
                .build();
        room2 = Room.builder()
                .title("스터디룸2")
                .description("설명2")
                .createdBy(user2)
                .build();
        roomRepository.saveAll(List.of(room1, room2));

        // user1은 room1의 멤버
        RoomMember member1 = RoomMember.createHost(room1, user1);
        // user1은 room2의 멤버
        RoomMember member2 = RoomMember.createMember(room2, user1);
        roomMemberRepository.saveAll(List.of(member1, member2));

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByUserIdOrSystemType 테스트")
    class FindByUserIdOrSystemTypeTest {

        @Test
        @DisplayName("개인 알림 조회")
        void t1() {
            // given
            Notification personalNotification = Notification.createPersonalNotification(
                    user1, "개인 알림", "내용", "/target"
            );
            notificationRepository.save(personalNotification);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findByUserIdOrSystemType(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getType())
                    .isEqualTo(NotificationType.PERSONAL);
            assertThat(result.getContent().get(0).getTitle())
                    .isEqualTo("개인 알림");
        }

        @Test
        @DisplayName("시스템 알림 조회")
        void t2() {
            // given
            Notification systemNotification = Notification.createSystemNotification(
                    "시스템 알림", "전체 공지", "/notice"
            );
            notificationRepository.save(systemNotification);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findByUserIdOrSystemType(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getType())
                    .isEqualTo(NotificationType.SYSTEM);
        }

        @Test
        @DisplayName("내가 멤버인 스터디룸 알림 조회")
        void t3() {
            // given
            // user1이 속한 room1, room2의 알림
            Notification roomNotification1 = Notification.createRoomNotification(
                    room1, "스터디룸1 알림", "내용", "/room/1"
            );
            Notification roomNotification2 = Notification.createRoomNotification(
                    room2, "스터디룸2 알림", "내용", "/room/2"
            );
            notificationRepository.saveAll(List.of(roomNotification1, roomNotification2));

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findByUserIdOrSystemType(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .extracting(Notification::getType)
                    .containsOnly(NotificationType.ROOM);
        }

        @Test
        @DisplayName("내가 멤버가 아닌 스터디룸 알림 조회 불가")
        void t4() {
            // given
            Room otherRoom = Room.builder()
                    .title("다른 방")
                    .description("설명")
                    .createdBy(user2)
                    .build();
            roomRepository.save(otherRoom);

            Notification otherRoomNotification = Notification.createRoomNotification(
                    otherRoom, "다른 방 알림", "내용", "/room/other"
            );
            notificationRepository.save(otherRoomNotification);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findByUserIdOrSystemType(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("모든 타입의 알림 함께 조회")
        void t5() {
            // given
            Notification personal = Notification.createPersonalNotification(
                    user1, "개인", "내용", "/p"
            );
            Notification system = Notification.createSystemNotification(
                    "시스템", "내용", "/s"
            );
            Notification room = Notification.createRoomNotification(
                    room1, "스터디룸", "내용", "/r"
            );
            Notification community = Notification.createCommunityNotification(
                    user1, "커뮤니티", "내용", "/c"
            );
            notificationRepository.saveAll(List.of(personal, system, room, community));

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findByUserIdOrSystemType(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(4);
            assertThat(result.getContent())
                    .extracting(Notification::getType)
                    .containsExactlyInAnyOrder(
                            NotificationType.PERSONAL,
                            NotificationType.SYSTEM,
                            NotificationType.ROOM,
                            NotificationType.COMMUNITY
                    );
        }

        @Test
        @DisplayName("생성일시 내림차순 정렬")
        void t6() throws InterruptedException {
            // given
            Notification old = Notification.createPersonalNotification(
                    user1, "오래된 알림", "내용", "/old"
            );
            notificationRepository.save(old);

            Thread.sleep(100); // 시간 차이를 위한 대기

            Notification recent = Notification.createPersonalNotification(
                    user1, "최근 알림", "내용", "/recent"
            );
            notificationRepository.save(recent);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findByUserIdOrSystemType(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("최근 알림");
            assertThat(result.getContent().get(1).getTitle()).isEqualTo("오래된 알림");
        }

        @Test
        @DisplayName("페이징 정상 작동")
        void t7() {
            // given
            for (int i = 1; i <= 15; i++) {
                Notification notification = Notification.createPersonalNotification(
                        user1, "알림 " + i, "내용", "/target"
                );
                notificationRepository.save(notification);
            }

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findByUserIdOrSystemType(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(10);
            assertThat(result.getTotalElements()).isEqualTo(15);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countUnreadByUserId 테스트")
    class CountUnreadByUserIdTest {

        @Test
        @DisplayName("읽지 않은 개인 알림 개수 조회")
        void t1() {
            // given
            Notification notification1 = Notification.createPersonalNotification(
                    user1, "알림1", "내용", "/1"
            );
            Notification notification2 = Notification.createPersonalNotification(
                    user1, "알림2", "내용", "/2"
            );
            notificationRepository.saveAll(List.of(notification1, notification2));

            // when
            long count = notificationRepository.countUnreadByUserId(user1.getId());

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("읽은 알림은 알림 개수에 포함 X")
        void t2() {
            // given
            Notification notification1 = Notification.createPersonalNotification(
                    user1, "알림1", "내용", "/1"
            );
            Notification notification2 = Notification.createPersonalNotification(
                    user1, "알림2", "내용", "/2"
            );
            notificationRepository.saveAll(List.of(notification1, notification2));

            // notification1을 읽음 처리
            NotificationRead read = NotificationRead.create(notification1, user1);
            notificationReadRepository.save(read);

            // when
            long count = notificationRepository.countUnreadByUserId(user1.getId());

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("시스템 알림도 읽지 않은 개수에 포함")
        void t3() {
            // given
            Notification systemNotification = Notification.createSystemNotification(
                    "시스템 알림", "내용", "/system"
            );
            notificationRepository.save(systemNotification);

            // when
            long count = notificationRepository.countUnreadByUserId(user1.getId());

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("스터디룸 알림도 읽지 않은 개수에 포함")
        void t4() {
            // given
            Notification roomNotification = Notification.createRoomNotification(
                    room1, "스터디룸 알림", "내용", "/room"
            );
            notificationRepository.save(roomNotification);

            // when
            long count = notificationRepository.countUnreadByUserId(user1.getId());

            // then
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findUnreadByUserId 테스트")
    class FindUnreadByUserIdTest {

        @Test
        @DisplayName("읽지 않은 알림만 조회")
        void t1() {
            // given
            Notification unread = Notification.createPersonalNotification(
                    user1, "읽지 않음", "내용", "/unread"
            );
            Notification read = Notification.createPersonalNotification(
                    user1, "읽음", "내용", "/read"
            );
            notificationRepository.saveAll(List.of(unread, read));

            // read 알림을 읽음 처리
            NotificationRead notificationRead = NotificationRead.create(read, user1);
            notificationReadRepository.save(notificationRead);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findUnreadByUserId(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("읽지 않음");
        }

        @Test
        @DisplayName("모든 타입의 읽지 않은 알림 조회")
        void t2() {
            // given
            Notification personal = Notification.createPersonalNotification(
                    user1, "개인", "내용", "/p"
            );
            Notification system = Notification.createSystemNotification(
                    "시스템", "내용", "/s"
            );
            Notification room = Notification.createRoomNotification(
                    room1, "스터디룸", "내용", "/r"
            );
            notificationRepository.saveAll(List.of(personal, system, room));

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findUnreadByUserId(user1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(3);
        }

        @Test
        @DisplayName("생성일시 내림차순으로 정렬")
        void t3() throws InterruptedException {
            // given
            Notification old = Notification.createPersonalNotification(
                    user1, "오래된", "내용", "/old"
            );
            notificationRepository.save(old);

            Thread.sleep(100);

            Notification recent = Notification.createPersonalNotification(
                    user1, "최근", "내용", "/recent"
            );
            notificationRepository.save(recent);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findUnreadByUserId(user1.getId(), pageable);

            // then
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("최근");
            assertThat(result.getContent().get(1).getTitle()).isEqualTo("오래된");
        }
    }

    @Nested
    @DisplayName("단순 쿼리 메서드 테스트")
    class SimpleQueryMethodTest {

        @Test
        @DisplayName("특정 스터디룸의 알림 조회")
        void t1() {
            // given
            Notification notification1 = Notification.createRoomNotification(
                    room1, "방1 알림", "내용", "/1"
            );
            Notification notification2 = Notification.createRoomNotification(
                    room2, "방2 알림", "내용", "/2"
            );
            notificationRepository.saveAll(List.of(notification1, notification2));

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Notification> result = notificationRepository
                    .findByRoomIdOrderByCreatedAtDesc(room1.getId(), pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("방1 알림");
        }

        @Test
        @DisplayName("특정 타입의 알림 조회")
        void t2() {
            // given
            Notification system1 = Notification.createSystemNotification(
                    "시스템1", "내용", "/1"
            );
            Notification system2 = Notification.createSystemNotification(
                    "시스템2", "내용", "/2"
            );
            Notification personal = Notification.createPersonalNotification(
                    user1, "개인", "내용", "/p"
            );
            notificationRepository.saveAll(List.of(system1, system2, personal));

            // when
            List<Notification> result = notificationRepository
                    .findByType(NotificationType.SYSTEM);

            // then
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(Notification::getType)
                    .containsOnly(NotificationType.SYSTEM);
        }
    }
}