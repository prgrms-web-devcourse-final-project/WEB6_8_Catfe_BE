package com.back.domain.user.service;

import com.back.domain.user.dto.UpdateUserProfileRequest;
import com.back.domain.user.dto.UserDetailResponse;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserProfileRepository;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ====================== 사용자 정보 조회 테스트 ======================

    @Test
    @DisplayName("정상 유저 정보 조회 성공")
    void getUserInfo_success() {
        // given: 정상 상태의 유저 저장
        User user = User.createUser("testuser", "test@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when: 서비스 호출
        UserDetailResponse response = userService.getUserInfo(user.getId());

        // then: 응답 값 검증
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.profile().nickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("유저 없음 → USER_NOT_FOUND 예외")
    void getUserInfo_userNotFound() {
        // when & then: 존재하지 않는 ID로 조회
        assertThatThrownBy(() -> userService.getUserInfo(999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("탈퇴 유저 조회 → USER_DELETED 예외")
    void getUserInfo_deletedUser() {
        // given: 상태 DELETED 유저 저장
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> userService.getUserInfo(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지 유저 조회 → USER_SUSPENDED 예외")
    void getUserInfo_suspendedUser() {
        // given: 상태 SUSPENDED 유저 저장
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> userService.getUserInfo(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    // ====================== 사용자 프로필 수정 테스트 ======================

    @Test
    @DisplayName("프로필 수정 성공")
    void updateUserProfile_success() {
        // given: 정상 유저 저장
        User user = User.createUser("updateuser", "update@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "기존닉", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "새닉네임", "https://cdn.example.com/new.png", "자기소개", LocalDate.of(1999, 5, 10)
        );

        // when: 서비스 호출
        UserDetailResponse response = userService.updateUserProfile(user.getId(), request);

        // then: 응답 및 DB 값 검증
        assertThat(response.profile().nickname()).isEqualTo("새닉네임");
        assertThat(response.profile().bio()).isEqualTo("자기소개");

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getUserProfile().getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("중복 닉네임 수정 → NICKNAME_DUPLICATED 예외")
    void updateUserProfile_duplicateNickname() {
        // given: user1, user2 저장
        User user1 = User.createUser("user1", "user1@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user1.setUserProfile(new UserProfile(user1, "닉1", null, null, null, 0));
        user1.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user1);

        User user2 = User.createUser("user2", "user2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user2.setUserProfile(new UserProfile(user2, "닉2", null, null, null, 0));
        user2.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user2);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("닉1", null, null, null);

        // when & then
        assertThatThrownBy(() -> userService.updateUserProfile(user2.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NICKNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("탈퇴 유저 프로필 수정 → USER_DELETED 예외")
    void updateUserProfile_deletedUser() {
        // given: 상태 DELETED 유저 저장
        User user = User.createUser("deleted2", "deleted2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새닉", null, null, null);

        // when & then
        assertThatThrownBy(() -> userService.updateUserProfile(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지 유저 프로필 수정 → USER_SUSPENDED 예외")
    void updateUserProfile_suspendedUser() {
        // given: 상태 SUSPENDED 유저 저장
        User user = User.createUser("suspended2", "suspended2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새닉", null, null, null);

        // when & then
        assertThatThrownBy(() -> userService.updateUserProfile(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    // ====================== 사용자 탈퇴 테스트 ======================

    @Test
    @DisplayName("정상 회원 탈퇴 성공")
    void deleteUser_success() {
        // given: 정상 상태의 유저 저장
        User user = User.createUser("deleteuser", "delete@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", "https://cdn.example.com/profile.png", "소개글", LocalDate.of(1995, 3, 15), 500));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // when: 탈퇴 처리
        userService.deleteUser(user.getId());

        // then: 상태 및 개인정보 마스킹 검증
        User deleted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deleted.getUserStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(deleted.getUsername()).startsWith("deleted_");
        assertThat(deleted.getEmail()).startsWith("deleted_");
        assertThat(deleted.getProvider()).startsWith("deleted_");
        assertThat(deleted.getProviderId()).startsWith("deleted_");

        UserProfile profile = deleted.getUserProfile();
        assertThat(profile.getNickname()).isEqualTo("탈퇴한 회원");
        assertThat(profile.getProfileImageUrl()).isNull();
        assertThat(profile.getBio()).isNull();
        assertThat(profile.getBirthDate()).isNull();
    }

    @Test
    @DisplayName("이미 탈퇴된 회원 탈퇴 시도 → USER_ALREADY_DELETED 예외")
    void deleteUser_alreadyDeleted() {
        // given: 상태 DELETED 유저 저장
        User user = User.createUser("deleteduser", "deleteduser@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("정지된 회원 탈퇴 시도 → USER_SUSPENDED 예외")
    void deleteUser_suspendedUser() {
        // given: 상태 SUSPENDED 유저 저장
        User user = User.createUser("suspendeduser", "suspendeduser@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(user.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_SUSPENDED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 회원 탈퇴 시도 → USER_NOT_FOUND 예외")
    void deleteUser_notFound() {
        // when & then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

}
