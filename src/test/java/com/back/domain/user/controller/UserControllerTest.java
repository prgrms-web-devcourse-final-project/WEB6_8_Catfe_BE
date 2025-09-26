package com.back.domain.user.controller;

import com.back.domain.user.dto.UpdateUserProfileRequest;
import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import com.back.fixture.TestJwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    // ====================== 내 정보 조회 테스트 ======================

    @Test
    @DisplayName("내 정보 조회 성공 → 200 OK")
    void getMyInfo_success() throws Exception {
        // given: 정상 유저 생성
        User user = User.createUser("myinfo", "myinfo@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", "https://cdn.example.com/1.png", "안녕하세요", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when: 내 정보 조회 요청
        ResultActions resultActions = mvc.perform(
                get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
        ).andDo(print());

        // then: 200 OK + 반환 값 검증
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("myinfo"))
                .andExpect(jsonPath("$.data.profile.nickname").value("홍길동"));
    }

    @Test
    @DisplayName("탈퇴한 계정 조회 → 410 Gone")
    void getMyInfo_deletedUser() throws Exception {
        // given: 상태 DELETED 유저 저장
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then: 410 Gone + USER_009
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("USER_009"))
                .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다."));
    }

    @Test
    @DisplayName("정지된 계정 조회 → 403 Forbidden")
    void getMyInfo_suspendedUser() throws Exception {
        // given: 상태 SUSPENDED 유저 저장
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then: 403 Forbidden + USER_008
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_008"))
                .andExpect(jsonPath("$.message").value("정지된 계정입니다. 관리자에게 문의하세요."));
    }

    @Test
    @DisplayName("AccessToken 없음 → 401 Unauthorized")
    void getMyInfo_noAccessToken() throws Exception {
        // when & then: 401 Unauthorized + AUTH_001
        mvc.perform(get("/api/users/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 AccessToken → 401 Unauthorized")
    void getMyInfo_invalidAccessToken() throws Exception {
        // when & then: 401 Unauthorized + AUTH_002
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer invalidToken"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 AccessToken → 401 Unauthorized")
    void getMyInfo_expiredAccessToken() throws Exception {
        // given: 만료된 토큰 발급
        User user = User.createUser("expired", "expired@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String expiredToken = testJwtTokenProvider.createExpiredAccessToken(user.getId(), user.getUsername(), user.getRole().name());

        // when & then: 401 Unauthorized + AUTH_004
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expiredToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"))
                .andExpect(jsonPath("$.message").value("만료된 액세스 토큰입니다."));
    }

    // ====================== 내 프로필 수정 테스트 ======================

    @Test
    @DisplayName("내 프로필 수정 성공 → 200 OK")
    void updateMyProfile_success() throws Exception {
        // given: 정상 유저 저장
        User user = User.createUser("updateuser", "update@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "기존닉", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "새닉네임",
                "https://cdn.example.com/profile/new.png",
                "저는 개발자입니다!",
                LocalDate.of(2000, 5, 10)
        );

        // when: 정상 프로필 수정 요청
        ResultActions resultActions = mvc.perform(
                patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andDo(print());

        // then: 200 OK + 변경된 값 검증
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 정보를 수정했습니다."))
                .andExpect(jsonPath("$.data.profile.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.data.profile.bio").value("저는 개발자입니다!"))
                .andExpect(jsonPath("$.data.profile.birthDate").value("2000-05-10"));
    }

    @Test
    @DisplayName("중복 닉네임 수정 → 409 Conflict")
    void updateMyProfile_duplicateNickname() throws Exception {
        // given: user1, user2 저장 (닉네임 중복 상황)
        User user1 = User.createUser("user1", "user1@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user1.setUserProfile(new UserProfile(user1, "닉1", null, null, null, 0));
        user1.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user1);

        User user2 = User.createUser("user2", "user2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user2.setUserProfile(new UserProfile(user2, "닉2", null, null, null, 0));
        user2.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user2);

        String accessToken = generateAccessToken(user2);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("닉1", null, null, null);

        // when & then
        mvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_004"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("탈퇴 계정 프로필 수정 → 410 Gone")
    void updateMyProfile_deletedUser() throws Exception {
        // given: 상태 DELETED 유저 저장
        User user = User.createUser("deleted2", "deleted2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새닉", null, null, null);

        // when & then
        mvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("USER_009"))
                .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다."));
    }

    @Test
    @DisplayName("정지 계정 프로필 수정 → 403 Forbidden")
    void updateMyProfile_suspendedUser() throws Exception {
        // given: 상태 SUSPENDED 유저 저장
        User user = User.createUser("suspended2", "suspended2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새닉", null, null, null);

        // when & then
        mvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_008"))
                .andExpect(jsonPath("$.message").value("정지된 계정입니다. 관리자에게 문의하세요."));
    }

    @Test
    @DisplayName("AccessToken 없음으로 프로필 수정 → 401 Unauthorized (AUTH_001)")
    void updateMyProfile_noAccessToken() throws Exception {
        // given: 요청 바디 준비
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새닉", null, null, null);

        // when & then
        mvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 AccessToken으로 프로필 수정 → 401 Unauthorized (AUTH_002)")
    void updateMyProfile_invalidAccessToken() throws Exception {
        // given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새닉", null, null, null);

        // when & then
        mvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer invalidToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 AccessToken으로 프로필 수정 → 401 Unauthorized (AUTH_004)")
    void updateMyProfile_expiredAccessToken() throws Exception {
        // given: 만료된 토큰 발급
        User user = User.createUser("expired2", "expired2@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String expiredToken = testJwtTokenProvider.createExpiredAccessToken(
                user.getId(), user.getUsername(), user.getRole().name()
        );

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새닉", null, null, null);

        // when & then
        mvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"))
                .andExpect(jsonPath("$.message").value("만료된 액세스 토큰입니다."));
    }
}
