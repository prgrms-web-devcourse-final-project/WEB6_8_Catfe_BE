package com.back.domain.studyroom.repository;

import com.back.domain.studyroom.entity.*;
import com.back.domain.user.entity.Role;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.global.config.QueryDslTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(QueryDslTestConfig.class)
@DisplayName("RoomMemberAvatarRepository 테스트")
class RoomMemberAvatarRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private RoomMemberAvatarRepository roomMemberAvatarRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    private Room testRoom1;
    private Room testRoom2;
    private User user1;
    private User user2;
    private User user3;
    private Avatar avatar1;
    private Avatar avatar2;
    private Avatar avatar3;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        user1 = createUser("user1", "user1@test.com", "유저1");
        user2 = createUser("user2", "user2@test.com", "유저2");
        user3 = createUser("user3", "user3@test.com", "유저3");

        // 방 생성
        testRoom1 = Room.create(
                "테스트 방 1",
                "테스트 설명",
                false,
                null,
                10,
                user1,
                null,
                true,
                null
        );
        testEntityManager.persist(testRoom1);

        testRoom2 = Room.create(
                "테스트 방 2",
                "테스트 설명",
                false,
                null,
                10,
                user1,
                null,
                true,
                null
        );
        testEntityManager.persist(testRoom2);

        // 아바타 생성
        avatar1 = createAvatar("검은 고양이", 1);
        avatar2 = createAvatar("하얀 고양이", 2);
        avatar3 = createAvatar("노란 고양이", 3);
        
        testEntityManager.flush();
        testEntityManager.clear();
    }

    private User createUser(String username, String email, String nickname) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password("password123")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        
        UserProfile profile = new UserProfile();
        profile.setNickname(nickname);
        user.setUserProfile(profile);
        
        testEntityManager.persist(user);
        return user;
    }

    private Avatar createAvatar(String name, int sortOrder) {
        Avatar avatar = Avatar.builder()
                .name(name)
                .imageUrl("/images/avatars/" + name + ".png")
                .description(name)
                .isDefault(true)
                .sortOrder(sortOrder)
                .category("CAT")
                .build();
        testEntityManager.persist(avatar);
        return avatar;
    }

    @Test
    @DisplayName("방별 아바타 설정 저장 및 조회")
    void saveAndFindByRoomIdAndUserId() {
        // given
        RoomMemberAvatar roomAvatar = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user1)
                .selectedAvatar(avatar1)
                .build();

        // when
        testEntityManager.persist(roomAvatar);
        testEntityManager.flush();
        testEntityManager.clear();
        
        Optional<RoomMemberAvatar> found = roomMemberAvatarRepository
                .findByRoomIdAndUserId(testRoom1.getId(), user1.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSelectedAvatar().getName()).isEqualTo("검은 고양이");
        assertThat(found.get().getRoom().getId()).isEqualTo(testRoom1.getId());
        assertThat(found.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("같은 방에서 여러 사용자의 아바타 일괄 조회 (N+1 방지)")
    void findByRoomIdAndUserIdIn() {
        // given
        RoomMemberAvatar avatar1Setting = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user1)
                .selectedAvatar(avatar1)
                .build();
        testEntityManager.persist(avatar1Setting);

        RoomMemberAvatar avatar2Setting = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user2)
                .selectedAvatar(avatar2)
                .build();
        testEntityManager.persist(avatar2Setting);

        RoomMemberAvatar avatar3Setting = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user3)
                .selectedAvatar(avatar3)
                .build();
        testEntityManager.persist(avatar3Setting);

        testEntityManager.flush();
        testEntityManager.clear();

        // when
        Set<Long> userIds = Set.of(user1.getId(), user2.getId(), user3.getId());
        List<RoomMemberAvatar> results = roomMemberAvatarRepository
                .findByRoomIdAndUserIdIn(testRoom1.getId(), userIds);

        // then
        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting(rma -> rma.getSelectedAvatar().getName())
                .containsExactlyInAnyOrder("검은 고양이", "하얀 고양이", "노란 고양이");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 - 빈 리스트 반환")
    void findByRoomIdAndUserIdIn_NotFound() {
        // when
        Set<Long> userIds = Set.of(999L, 1000L);
        List<RoomMemberAvatar> results = roomMemberAvatarRepository
                .findByRoomIdAndUserIdIn(testRoom1.getId(), userIds);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("아바타 변경 (업데이트)")
    void updateSelectedAvatar() {
        // given
        RoomMemberAvatar roomAvatar = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user1)
                .selectedAvatar(avatar1)
                .build();
        testEntityManager.persist(roomAvatar);
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        RoomMemberAvatar found = roomMemberAvatarRepository
                .findByRoomIdAndUserId(testRoom1.getId(), user1.getId())
                .orElseThrow();
        found.setSelectedAvatar(avatar2);
        testEntityManager.persist(found);
        testEntityManager.flush();
        testEntityManager.clear();

        // then
        RoomMemberAvatar updated = roomMemberAvatarRepository
                .findByRoomIdAndUserId(testRoom1.getId(), user1.getId())
                .orElseThrow();

        assertThat(updated.getSelectedAvatar().getName()).isEqualTo("하얀 고양이");
    }

    @Test
    @DisplayName("사용자가 여러 방에서 각기 다른 아바타 설정")
    void userCanHaveDifferentAvatarsInDifferentRooms() {
        // given
        RoomMemberAvatar room1Avatar = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user1)
                .selectedAvatar(avatar1)
                .build();
        testEntityManager.persist(room1Avatar);

        RoomMemberAvatar room2Avatar = RoomMemberAvatar.builder()
                .room(testRoom2)
                .user(user1)
                .selectedAvatar(avatar3)
                .build();
        testEntityManager.persist(room2Avatar);

        testEntityManager.flush();
        testEntityManager.clear();

        // when
        RoomMemberAvatar found1 = roomMemberAvatarRepository
                .findByRoomIdAndUserId(testRoom1.getId(), user1.getId())
                .orElseThrow();
        
        RoomMemberAvatar found2 = roomMemberAvatarRepository
                .findByRoomIdAndUserId(testRoom2.getId(), user1.getId())
                .orElseThrow();

        // then
        assertThat(found1.getSelectedAvatar().getName()).isEqualTo("검은 고양이");
        assertThat(found2.getSelectedAvatar().getName()).isEqualTo("노란 고양이");
    }

    @Test
    @DisplayName("아바타 설정 삭제")
    void deleteRoomMemberAvatar() {
        // given
        RoomMemberAvatar roomAvatar = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user1)
                .selectedAvatar(avatar1)
                .build();
        testEntityManager.persist(roomAvatar);
        testEntityManager.flush();

        // when
        roomMemberAvatarRepository.delete(roomAvatar);
        testEntityManager.flush();
        testEntityManager.clear();

        // then
        Optional<RoomMemberAvatar> found = roomMemberAvatarRepository
                .findByRoomIdAndUserId(testRoom1.getId(), user1.getId());
        
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Fetch Join으로 아바타 정보 한 번에 조회 (N+1 방지)")
    void fetchJoinTest() {
        // given
        RoomMemberAvatar avatar1Setting = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user1)
                .selectedAvatar(avatar1)
                .build();
        testEntityManager.persist(avatar1Setting);

        RoomMemberAvatar avatar2Setting = RoomMemberAvatar.builder()
                .room(testRoom1)
                .user(user2)
                .selectedAvatar(avatar2)
                .build();
        testEntityManager.persist(avatar2Setting);

        testEntityManager.flush();
        testEntityManager.clear();

        // when
        Set<Long> userIds = Set.of(user1.getId(), user2.getId());
        List<RoomMemberAvatar> results = roomMemberAvatarRepository
                .findByRoomIdAndUserIdIn(testRoom1.getId(), userIds);

        // then
        assertThat(results).hasSize(2);
        results.forEach(rma -> {
            assertThat(rma.getSelectedAvatar()).isNotNull();
            assertThat(rma.getSelectedAvatar().getName()).isNotBlank();
        });
    }
}
