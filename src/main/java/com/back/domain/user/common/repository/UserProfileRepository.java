package com.back.domain.user.common.repository;

import com.back.domain.user.common.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);
}
