package com.back.domain.studyroom.integration;

import com.back.domain.studyroom.entity.Room;
import com.back.domain.studyroom.repository.RoomMemberRepository;
import com.back.domain.studyroom.repository.RoomRepository;
import com.back.domain.studyroom.service.RoomService;
import com.back.domain.user.common.enums.Role;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("방 생성 통합 테스트 - 실제 DB 저장")
public class RoomCreateIntegrationTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 실제 사용자 생성 및 저장
        UserProfile userProfile = new UserProfile();
        userProfile.setNickname("테스트유저");
        userProfile.setProfileImageUrl("https://example.com/profile.jpg");

        testUser = User.builder()
                .username("testuser@test.com")
                .email("testuser@test.com")
                .password("password123")
                .role(Role.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();
        
        testUser.setUserProfile(userProfile);
        testUser = userRepository.save(testUser);
        
        System.out.println("=== 테스트 사용자 생성 완료: ID = " + testUser.getId());
    }

    @Test
    @DisplayName("방 생성 실제 저장 테스트 - 프론트엔드 요청과 동일한 조건")
    void createRoom_RealSave() {
        // given - 프론트엔드가 보낸 것과 동일한 요청
        String title = "테스트1";
        String description = "테스트 제발 제발 제";
        boolean isPrivate = true;
        String password = "123123123";
        int maxParticipants = 9;
        boolean useWebRTC = false;

        System.out.println("=== 방 생성 시작 ===");
        System.out.println("Title: " + title);
        System.out.println("Description: " + description);
        System.out.println("IsPrivate: " + isPrivate);
        System.out.println("Password: " + password);
        System.out.println("MaxParticipants: " + maxParticipants);
        System.out.println("UseWebRTC: " + useWebRTC);
        System.out.println("CreatorId: " + testUser.getId());

        // when - 실제 서비스 호출
        Room createdRoom = roomService.createRoom(
                title,
                description,
                isPrivate,
                password,
                maxParticipants,
                testUser.getId(),
                useWebRTC,
                null  // thumbnailAttachmentId
        );

        // then
        assertThat(createdRoom).isNotNull();
        assertThat(createdRoom.getId()).isNotNull();
        assertThat(createdRoom.getTitle()).isEqualTo(title);
        assertThat(createdRoom.getDescription()).isEqualTo(description);
        assertThat(createdRoom.isPrivate()).isEqualTo(isPrivate);
        assertThat(createdRoom.getPassword()).isEqualTo(password);
        assertThat(createdRoom.getMaxParticipants()).isEqualTo(maxParticipants);
        assertThat(createdRoom.isAllowCamera()).isEqualTo(useWebRTC);
        assertThat(createdRoom.isAllowAudio()).isEqualTo(useWebRTC);
        assertThat(createdRoom.isAllowScreenShare()).isEqualTo(useWebRTC);

        System.out.println("=== 방 생성 성공! ===");
        System.out.println("Room ID: " + createdRoom.getId());
        System.out.println("Room Members: " + createdRoom.getRoomMembers());
        System.out.println("Room Chat Messages: " + createdRoom.getRoomChatMessages());
        
        // DB에 실제로 저장되었는지 확인
        Room savedRoom = roomRepository.findById(createdRoom.getId()).orElse(null);
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getTitle()).isEqualTo(title);
        
        // 방장 멤버도 저장되었는지 확인
        boolean hostExists = roomMemberRepository.existsByRoomIdAndUserId(
                createdRoom.getId(), 
                testUser.getId()
        );
        assertThat(hostExists).isTrue();
        
        System.out.println("=== 방 생성 통합 테스트 완료 ===");
    }

    @Test
    @DisplayName("방 생성 - 컬렉션 필드 null 체크")
    void createRoom_CheckCollections() {
        // given
        String title = "컬렉션 테스트";
        
        // when
        Room createdRoom = roomService.createRoom(
                title,
                "설명",
                false,
                null,
                10,
                testUser.getId(),
                true,  // useWebRTC
                null   // thumbnailAttachmentId
        );

        // then - 컬렉션 필드들이 null이 아니어야 함
        assertThat(createdRoom.getRoomMembers()).isNotNull();
        assertThat(createdRoom.getRoomChatMessages()).isNotNull();
        assertThat(createdRoom.getRoomParticipantHistories()).isNotNull();
        assertThat(createdRoom.getStudyRecords()).isNotNull();
        
        System.out.println("=== 컬렉션 필드 확인 ===");
        System.out.println("RoomMembers: " + createdRoom.getRoomMembers());
        System.out.println("RoomChatMessages: " + createdRoom.getRoomChatMessages());
        System.out.println("RoomParticipantHistories: " + createdRoom.getRoomParticipantHistories());
        System.out.println("StudyRecords: " + createdRoom.getStudyRecords());
    }
}
