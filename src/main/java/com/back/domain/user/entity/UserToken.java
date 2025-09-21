package com.back.domain.user.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class UserToken extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String refreshToken;

    private LocalDateTime expiredAt;
}
