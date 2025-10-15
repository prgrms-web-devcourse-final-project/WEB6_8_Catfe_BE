package com.back.domain.user.account.controller;

import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.comment.repository.CommentRepository;
import com.back.domain.board.post.entity.Post;
import com.back.domain.board.post.entity.PostBookmark;
import com.back.domain.board.post.repository.PostBookmarkRepository;
import com.back.domain.board.post.repository.PostRepository;
import com.back.domain.file.entity.FileAttachment;
import com.back.domain.file.repository.FileAttachmentRepository;
import com.back.domain.user.account.dto.ChangePasswordRequest;
import com.back.domain.user.account.dto.UserProfileRequest;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.entity.UserProfile;
import com.back.domain.user.common.enums.UserStatus;
import com.back.domain.user.common.repository.UserRepository;
import com.back.fixture.TestJwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    private TestJwtTokenProvider testJwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String generateAccessToken(User user) {
        return testJwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    private MultipartFile mockMultipartFile(String filename) {
        return new MockMultipartFile(filename, filename, "image/png", new byte[]{1, 2, 3});
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
                .andExpect(jsonPath("$.data.profile.nickname").value("홍길동"))
                .andExpect(jsonPath("$.data.profile.profileImageUrl").value("https://cdn.example.com/1.png"));
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

        FileAttachment attachment = new FileAttachment("profile_uuid_img.png", mockMultipartFile("profile.png"), user, "https://cdn.example.com/profile/new.png");
        fileAttachmentRepository.save(attachment);

        UserProfileRequest request = new UserProfileRequest(
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
                .andExpect(jsonPath("$.data.profile.birthDate").value("2000-05-10"))
                .andExpect(jsonPath("$.data.profile.profileImageUrl").value("https://cdn.example.com/profile/new.png"));
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

        UserProfileRequest request = new UserProfileRequest("닉1", null, null, null);

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

        UserProfileRequest request = new UserProfileRequest("새닉", null, null, null);

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

        UserProfileRequest request = new UserProfileRequest("새닉", null, null, null);

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
        UserProfileRequest request = new UserProfileRequest("새닉", null, null, null);

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
        UserProfileRequest request = new UserProfileRequest("새닉", null, null, null);

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

        UserProfileRequest request = new UserProfileRequest("새닉", null, null, null);

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

    // ====================== 내 비밀번호 변경 테스트 ======================

    @Test
    @DisplayName("비밀번호 변경 성공 → 200 OK")
    void changePassword_success() throws Exception {
        // given
        User user = User.createUser("changepw", "changepw@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, "소개글", LocalDate.of(2000, 1, 1), 1000));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        ChangePasswordRequest request = new ChangePasswordRequest("P@ssw0rd!", "NewP@ssw0rd!");

        // when & then
        mvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));

        // DB 값 검증
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewP@ssw0rd!", updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("현재 비밀번호 불일치 → 401 Unauthorized (USER_006)")
    void changePassword_invalidCurrentPassword() throws Exception {
        // given
        User user = User.createUser("wrongpw", "wrongpw@example.com", passwordEncoder.encode("Correct1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        ChangePasswordRequest request = new ChangePasswordRequest("Wrong1!", "NewP@ssw0rd!");

        // when & then
        mvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_006"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("새 비밀번호 정책 위반 → 400 Bad Request (USER_005)")
    void changePassword_invalidNewPassword() throws Exception {
        // given
        User user = User.createUser("invalidpw", "invalidpw@example.com", passwordEncoder.encode("Valid1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        ChangePasswordRequest request = new ChangePasswordRequest("Valid1!", "short");

        // when & then
        mvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_005"))
                .andExpect(jsonPath("$.message").value("비밀번호는 최소 8자 이상, 숫자/특수문자를 포함해야 합니다."));
    }

    @Test
    @DisplayName("소셜 로그인 회원 비밀번호 변경 시도 → 403 Forbidden (USER_010)")
    void changePassword_socialUser() throws Exception {
        User user = User.createUser("socialuser", "social@example.com", null);
        user.setProvider("kakao");
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        ChangePasswordRequest request = new ChangePasswordRequest("dummy", "NewP@ssw0rd!");

        mvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_010"))
                .andExpect(jsonPath("$.message").value("소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."));
    }

    @Test
    @DisplayName("탈퇴 계정 비밀번호 변경 시도 → 410 Gone (USER_009)")
    void changePassword_deletedUser() throws Exception {
        // given
        User user = User.createUser("deletedpw", "deletedpw@example.com", passwordEncoder.encode("Valid1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        ChangePasswordRequest request = new ChangePasswordRequest("Valid1!", "NewP@ssw0rd!");

        // when & then
        mvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("USER_009"))
                .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다."));
    }

    @Test
    @DisplayName("정지 계정 비밀번호 변경 시도 → 403 Forbidden (USER_008)")
    void changePassword_suspendedUser() throws Exception {
        // given
        User user = User.createUser("suspendedpw", "suspendedpw@example.com", passwordEncoder.encode("Valid1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        ChangePasswordRequest request = new ChangePasswordRequest("Valid1!", "NewP@ssw0rd!");

        // when & then
        mvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_008"))
                .andExpect(jsonPath("$.message").value("정지된 계정입니다. 관리자에게 문의하세요."));
    }

    @Test
    @DisplayName("AccessToken 없음으로 비밀번호 변경 시도 → 401 Unauthorized (AUTH_001)")
    void changePassword_noAccessToken() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("P@ssw0rd!", "NewP@ssw0rd!");

        mvc.perform(patch("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 AccessToken으로 비밀번호 변경 시도 → 401 Unauthorized (AUTH_002)")
    void changePassword_invalidAccessToken() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("P@ssw0rd!", "NewP@ssw0rd!");

        mvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer invalidToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 AccessToken으로 비밀번호 변경 시도 → 401 Unauthorized (AUTH_004)")
    void changePassword_expiredAccessToken() throws Exception {
        // given
        User user = User.createUser("expiredpw", "expiredpw@example.com", passwordEncoder.encode("Valid1!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String expiredToken = testJwtTokenProvider.createExpiredAccessToken(
                user.getId(), user.getUsername(), user.getRole().name()
        );

        ChangePasswordRequest request = new ChangePasswordRequest("Valid1!", "NewP@ssw0rd!");

        // when & then
        mvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"))
                .andExpect(jsonPath("$.message").value("만료된 액세스 토큰입니다."));
    }

    // ====================== 내 계정 삭제 테스트 ======================

    @Test
    @DisplayName("회원 탈퇴 성공 → 200 OK")
    void deleteMyAccount_success() throws Exception {
        // given: 정상 유저 저장
        User user = User.createUser("deleteuser", "delete@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", "https://cdn.example.com/1.png", "소개글", LocalDate.of(1990, 1, 1), 100));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));

        // DB 반영 확인
        User deleted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deleted.getUserStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(deleted.getUsername()).startsWith("deleted_");
        assertThat(deleted.getEmail()).startsWith("deleted_");
        assertThat(deleted.getProvider()).startsWith("deleted_");
        assertThat(deleted.getProviderId()).startsWith("deleted_");
        assertThat(deleted.getUserProfile().getNickname()).isEqualTo("탈퇴한 회원");
    }

    @Test
    @DisplayName("이미 탈퇴한 계정 탈퇴 시도 → 410 Gone")
    void deleteMyAccount_alreadyDeleted() throws Exception {
        // given: DELETED 상태 유저 저장
        User user = User.createUser("alreadydeleted", "already@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("USER_009"))
                .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다."));
    }

    @Test
    @DisplayName("정지된 계정 탈퇴 시도 → 403 Forbidden")
    void deleteMyAccount_suspendedUser() throws Exception {
        // given: SUSPENDED 상태 유저 저장
        User user = User.createUser("suspendeddelete", "suspendeddelete@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_008"))
                .andExpect(jsonPath("$.message").value("정지된 계정입니다. 관리자에게 문의하세요."));
    }

    @Test
    @DisplayName("AccessToken 없음으로 회원 탈퇴 시도 → 401 Unauthorized")
    void deleteMyAccount_noAccessToken() throws Exception {
        mvc.perform(delete("/api/users/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 AccessToken으로 회원 탈퇴 시도 → 401 Unauthorized (AUTH_002)")
    void deleteMyAccount_invalidAccessToken() throws Exception {
        mvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer invalidToken"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 AccessToken으로 회원 탈퇴 시도 → 401 Unauthorized (AUTH_004)")
    void deleteMyAccount_expiredAccessToken() throws Exception {
        // given
        User user = User.createUser("expiredDelete", "expireddelete@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String expiredToken = testJwtTokenProvider.createExpiredAccessToken(
                user.getId(), user.getUsername(), user.getRole().name()
        );

        // when & then
        mvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"))
                .andExpect(jsonPath("$.message").value("만료된 액세스 토큰입니다."));
    }

    // ====================== 내 게시글 목록 조회 테스트 ======================

    @Test
    @DisplayName("내 게시글 목록 조회 성공 → 200 OK")
    void getMyPosts_success() throws Exception {
        // given: 정상 유저 + 게시글 2개 생성
        User user = User.createUser("writer", "writer@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post1 = new Post(user, "첫 번째 글", "내용1", null);
        Post post2 = new Post(user, "두 번째 글", "내용2", null);
        postRepository.saveAll(List.of(post1, post2));

        String accessToken = generateAccessToken(user);

        // when
        ResultActions resultActions = mvc.perform(
                get("/api/users/me/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10")
        ).andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("내 게시글 목록이 조회되었습니다."))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[*].title").value(
                        containsInAnyOrder("첫 번째 글", "두 번째 글")
                ));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 → 404 Not Found")
    void getMyPosts_userNotFound() throws Exception {
        // given: 존재하지 않는 ID로 JWT 발급
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        // when & then
        mvc.perform(
                        get("/api/users/me/posts")
                                .header("Authorization", "Bearer " + fakeToken)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("탈퇴한 계정 → 410 Gone")
    void getMyPosts_deletedUser() throws Exception {
        // given
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(get("/api/users/me/posts")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("USER_009"))
                .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다."));
    }

    @Test
    @DisplayName("정지된 계정 → 403 Forbidden")
    void getMyPosts_suspendedUser() throws Exception {
        // given
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(get("/api/users/me/posts")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_008"))
                .andExpect(jsonPath("$.message").value("정지된 계정입니다. 관리자에게 문의하세요."));
    }

    @Test
    @DisplayName("AccessToken 없음 → 401 Unauthorized")
    void getMyPosts_noAccessToken() throws Exception {
        // when & then
        mvc.perform(get("/api/users/me/posts"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 AccessToken → 401 Unauthorized")
    void getMyPosts_invalidAccessToken() throws Exception {
        mvc.perform(get("/api/users/me/posts")
                        .header("Authorization", "Bearer invalidToken"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 AccessToken → 401 Unauthorized")
    void getMyPosts_expiredAccessToken() throws Exception {
        // given
        User user = User.createUser("expired", "expired@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String expiredToken = testJwtTokenProvider.createExpiredAccessToken(user.getId(), user.getUsername(), user.getRole().name());

        // when & then
        mvc.perform(get("/api/users/me/posts")
                        .header("Authorization", "Bearer " + expiredToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"))
                .andExpect(jsonPath("$.message").value("만료된 액세스 토큰입니다."));
    }

    // ====================== 내 댓글 목록 조회 테스트 ======================

    @Test
    @DisplayName("내 댓글 목록 조회 성공 → 200 OK")
    void getMyComments_success() throws Exception {
        // given: 정상 유저 + 게시글 + 댓글 2개 생성
        User user = User.createUser("commenter", "commenter@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post = new Post(user, "스프링 트랜잭션 정리", "내용입니다.", null);
        postRepository.save(post);

        Comment parent = new Comment(post, user, "코딩 박사의 스프링 교재도 추천합니다.", null);
        Comment comment1 = new Comment(post, user, "정말 도움이 많이 됐어요!", null);
        Comment comment2 = new Comment(post, user, "감사합니다! 더 공부해볼게요.", parent);
        commentRepository.saveAll(List.of(parent, comment1, comment2));

        String accessToken = generateAccessToken(user);

        // when
        ResultActions resultActions = mvc.perform(
                get("/api/users/me/comments")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10")
        ).andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("내 댓글 목록이 조회되었습니다."))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[*].content").value(
                        containsInAnyOrder("코딩 박사의 스프링 교재도 추천합니다.", "정말 도움이 많이 됐어요!", "감사합니다! 더 공부해볼게요.")
                ));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 → 404 Not Found")
    void getMyComments_userNotFound() throws Exception {
        // given
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        // when & then
        mvc.perform(get("/api/users/me/comments")
                        .header("Authorization", "Bearer " + fakeToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("탈퇴한 계정 → 410 Gone")
    void getMyComments_deletedUser() throws Exception {
        // given
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(get("/api/users/me/comments")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("USER_009"))
                .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다."));
    }

    @Test
    @DisplayName("정지된 계정 → 403 Forbidden")
    void getMyComments_suspendedUser() throws Exception {
        // given
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(get("/api/users/me/comments")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_008"))
                .andExpect(jsonPath("$.message").value("정지된 계정입니다. 관리자에게 문의하세요."));
    }

    @Test
    @DisplayName("AccessToken 없음 → 401 Unauthorized")
    void getMyComments_noAccessToken() throws Exception {
        // when & then
        mvc.perform(get("/api/users/me/comments"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 AccessToken → 401 Unauthorized")
    void getMyComments_invalidAccessToken() throws Exception {
        // when & then
        mvc.perform(get("/api/users/me/comments")
                        .header("Authorization", "Bearer invalidToken"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 AccessToken → 401 Unauthorized")
    void getMyComments_expiredAccessToken() throws Exception {
        // given
        User user = User.createUser("expired", "expired@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String expiredToken = testJwtTokenProvider.createExpiredAccessToken(user.getId(), user.getUsername(), user.getRole().name());

        // when & then
        mvc.perform(get("/api/users/me/comments")
                        .header("Authorization", "Bearer " + expiredToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"))
                .andExpect(jsonPath("$.message").value("만료된 액세스 토큰입니다."));
    }

    // ====================== 내 북마크 게시글 목록 조회 테스트 ======================

    @Test
    @DisplayName("내 북마크 게시글 목록 조회 성공 → 200 OK")
    void getMyBookmarks_success() throws Exception {
        // given
        User user = User.createUser("bookmarkUser", "bookmark@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "홍길동", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        Post post1 = new Post(user, "JPA 영속성 전이 완벽 정리", "내용1", null);
        Post post2 = new Post(user, "테스트 코드 작성 가이드", "내용2", null);
        postRepository.saveAll(List.of(post1, post2));

        PostBookmark bookmark1 = new PostBookmark(post1, user);
        PostBookmark bookmark2 = new PostBookmark(post2, user);
        postBookmarkRepository.saveAll(List.of(bookmark1, bookmark2));

        String accessToken = generateAccessToken(user);

        // when
        ResultActions resultActions = mvc.perform(
                        get("/api/users/me/bookmarks")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("page", "0")
                                .param("size", "10")
                )
                .andDo(print());

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_200"))
                .andExpect(jsonPath("$.message").value("내 북마크 게시글 목록이 조회되었습니다."))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[*].title").value(
                        containsInAnyOrder("JPA 영속성 전이 완벽 정리", "테스트 코드 작성 가이드")
                ));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 → 404 Not Found")
    void getMyBookmarks_userNotFound() throws Exception {
        // given
        String fakeToken = testJwtTokenProvider.createAccessToken(999L, "ghost", "USER");

        // when & then
        mvc.perform(get("/api/users/me/bookmarks")
                        .header("Authorization", "Bearer " + fakeToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
    }

    @Test
    @DisplayName("탈퇴한 계정 → 410 Gone")
    void getMyBookmarks_deletedUser() throws Exception {
        // given
        User user = User.createUser("deleted", "deleted@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(get("/api/users/me/bookmarks")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("USER_009"))
                .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다."));
    }

    @Test
    @DisplayName("정지된 계정 → 403 Forbidden")
    void getMyBookmarks_suspendedUser() throws Exception {
        // given
        User user = User.createUser("suspended", "suspended@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        String accessToken = generateAccessToken(user);

        // when & then
        mvc.perform(get("/api/users/me/bookmarks")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_008"))
                .andExpect(jsonPath("$.message").value("정지된 계정입니다. 관리자에게 문의하세요."));
    }

    @Test
    @DisplayName("AccessToken 없음 → 401 Unauthorized")
    void getMyBookmarks_noAccessToken() throws Exception {
        // when & then
        mvc.perform(get("/api/users/me/bookmarks"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("잘못된 AccessToken → 401 Unauthorized")
    void getMyBookmarks_invalidAccessToken() throws Exception {
        // when & then
        mvc.perform(get("/api/users/me/bookmarks")
                        .header("Authorization", "Bearer invalidToken"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 AccessToken → 401 Unauthorized")
    void getMyBookmarks_expiredAccessToken() throws Exception {
        // given
        User user = User.createUser("expired", "expired@example.com", passwordEncoder.encode("P@ssw0rd!"));
        user.setUserProfile(new UserProfile(user, "닉네임", null, null, null, 0));
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        String expiredToken = testJwtTokenProvider.createExpiredAccessToken(user.getId(), user.getUsername(), user.getRole().name());

        // when & then
        mvc.perform(get("/api/users/me/bookmarks")
                        .header("Authorization", "Bearer " + expiredToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"))
                .andExpect(jsonPath("$.message").value("만료된 액세스 토큰입니다."));
    }
}
