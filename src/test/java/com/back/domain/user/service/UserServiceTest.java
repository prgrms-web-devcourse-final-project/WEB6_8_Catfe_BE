package com.back.domain.user.service;

import com.back.domain.user.dto.UserRegisterRequest;
import com.back.domain.user.dto.UserResponse;
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

    @Test
    @DisplayName("정상 회원가입 성공")
    void register_success() {
        // given
        UserRegisterRequest request = new UserRegisterRequest(
                "testuser", "test@example.com", "P@ssw0rd!", "홍길동"
        );

        // when
        UserResponse response = userService.register(request);

        // then
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("홍길동");
        assertThat(response.status()).isEqualTo(UserStatus.PENDING);

        // 비밀번호 인코딩 검증
        String encoded = userRepository.findById(response.userId()).get().getPassword();
        assertThat(passwordEncoder.matches("P@ssw0rd!", encoded)).isTrue();

        // UserProfile도 함께 저장됐는지 검증
        assertThat(userProfileRepository.existsByNickname("홍길동")).isTrue();
    }

    @Test
    @DisplayName("중복된 username이면 예외 발생")
    void register_duplicateUsername() {
        userService.register(new UserRegisterRequest(
                "dupuser", "dup@example.com", "P@ssw0rd!", "닉네임"
        ));

        assertThatThrownBy(() ->
                userService.register(new UserRegisterRequest(
                        "dupuser", "other@example.com", "P@ssw0rd!", "다른닉네임"
                ))
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USERNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("중복된 email이면 예외 발생")
    void register_duplicateEmail() {
        userService.register(new UserRegisterRequest(
                "user1", "dup@example.com", "P@ssw0rd!", "닉네임"
        ));

        assertThatThrownBy(() ->
                userService.register(new UserRegisterRequest(
                        "user2", "dup@example.com", "P@ssw0rd!", "다른닉네임"
                ))
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.EMAIL_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("중복된 nickname이면 예외 발생")
    void register_duplicateNickname() {
        userService.register(new UserRegisterRequest(
                "user1", "user1@example.com", "P@ssw0rd!", "dupnick"
        ));

        assertThatThrownBy(() ->
                userService.register(new UserRegisterRequest(
                        "user2", "user2@example.com", "P@ssw0rd!", "dupnick"
                ))
        ).isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.NICKNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("비밀번호 정책 위반(숫자/특수문자 없음) → 예외 발생")
    void register_invalidPassword_noNumberOrSpecial() {
        UserRegisterRequest request = new UserRegisterRequest(
                "user1", "user1@example.com", "abcdefgh", "닉네임"
        );

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("비밀번호 정책 위반(길이 7자) → 예외 발생")
    void register_invalidPassword_tooShort() {
        UserRegisterRequest request = new UserRegisterRequest(
                "user2", "user2@example.com", "Abc12!", "닉네임"
        );

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("비밀번호 정책 통과(정상 8자 이상, 숫자/특수문자 포함) → 성공")
    void register_validPassword() {
        UserRegisterRequest request = new UserRegisterRequest(
                "user3", "user3@example.com", "Abcd123!", "닉네임"
        );

        UserResponse response = userService.register(request);

        assertThat(response.username()).isEqualTo("user3");
        assertThat(passwordEncoder.matches("Abcd123!",
                userRepository.findById(response.userId()).get().getPassword())).isTrue();
    }
}