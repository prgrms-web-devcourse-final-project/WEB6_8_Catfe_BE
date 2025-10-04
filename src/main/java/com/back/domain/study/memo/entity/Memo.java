package com.back.domain.study.memo.entity;

import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Memo extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String description;

    // setter 사용 안하고 메서드를 이용
    // 생성
    public static Memo create(User user, LocalDate date, String description) {
        Memo memo = new Memo();
        memo.user = user;
        memo.date = date;
        memo.description = description;
        return memo;
    }
    // 수정
    public void update(String description) {
        this.description = description;
    }
}
