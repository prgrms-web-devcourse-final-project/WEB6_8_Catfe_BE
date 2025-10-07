package com.back.global.initData;

import com.back.domain.user.entity.User;
import com.back.domain.user.entity.UserProfile;
import com.back.domain.user.entity.UserStatus;
import com.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class DevInitData {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner DevInitDataApplicationRunner() {
        return args -> {
            initUsers();
        };
    }

    @Transactional
    public void initUsers() {
        if (userRepository.count() == 0) {
            User admin = User.createAdmin(
                    "admin",
                    "admin@example.com",
                    passwordEncoder.encode("12345678!")
            );
            admin.setUserProfile(new UserProfile(admin, "관리자", null, null, null, 0));
            userRepository.save(admin);

            User user1 = User.createUser(
                    "user1",
                    "user1@example.com",
                    passwordEncoder.encode("12345678!")
            );
            user1.setUserProfile(new UserProfile(user1, "사용자1", null, null, null, 0));
            user1.setUserStatus(UserStatus.ACTIVE);
            userRepository.save(user1);

            User user2 = User.createUser(
                    "user2",
                    "user2@example.com",
                    passwordEncoder.encode("12345678!")
            );
            user2.setUserProfile(new UserProfile(user2, "사용자2", null, null, null, 0));
            user2.setUserStatus(UserStatus.ACTIVE);
            userRepository.save(user2);
        }
    }
}
