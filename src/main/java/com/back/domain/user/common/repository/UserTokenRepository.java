package com.back.domain.user.common.repository;

import com.back.domain.user.common.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByRefreshToken(String refreshToken);
    void deleteByRefreshToken(String refreshToken);
}
